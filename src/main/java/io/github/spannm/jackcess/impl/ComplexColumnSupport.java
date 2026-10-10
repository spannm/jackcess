/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.CursorBuilder;
import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.IndexCursor;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.complex.ComplexColumnInfo;
import io.github.spannm.jackcess.complex.ComplexValue;
import io.github.spannm.jackcess.impl.complex.AttachmentColumnInfoImpl;
import io.github.spannm.jackcess.impl.complex.MultiValueColumnInfoImpl;
import io.github.spannm.jackcess.impl.complex.UnsupportedColumnInfoImpl;
import io.github.spannm.jackcess.impl.complex.VersionHistoryColumnInfoImpl;
import io.github.spannm.jackcess.util.StringUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility code for loading complex columns.
 */
public final class ComplexColumnSupport {
    private static final Logger        LOGGER                     = Logger.getLogger(ComplexColumnSupport.class.getName());

    private static final String        COL_COMPLEX_TYPE_OBJECT_ID = "ComplexTypeObjectID";
    private static final String        COL_TABLE_ID               = "ConceptualTableID";
    private static final String        COL_FLAT_TABLE_ID          = "FlatTableID";
    private static final String        COL_COLUMN_NAME            = "ColumnName";

    /**
     * Prefix of the type table of a multi-value or attachment complex column. These tables are shared, one per
     * database, and ms access creates them whether or not anything uses them.
     */
    private static final String        COMPLEX_TYPE_PREFIX         = "MSysComplexType_";
    /** the type table of every attachment complex column */
    private static final String        ATTACHMENT_TYPE_TABLE       = COMPLEX_TYPE_PREFIX + "Attachment";
    /**
     * Prefix of the type table of a version history complex column. Unlike the others these are created per column,
     * which is why the name carries a guid.
     */
    private static final String        VERSION_HISTORY_TYPE_PREFIX = "MSysComplexTypeVH_";

    private static final Set<DataType> MULTI_VALUE_TYPES          = EnumSet.of(
        DataType.BYTE, DataType.INT, DataType.LONG, DataType.FLOAT,
        DataType.DOUBLE, DataType.GUID, DataType.NUMERIC, DataType.TEXT,
        DataType.BIG_INT);

    private ComplexColumnSupport() {
    }

    /**
     * Creates a ComplexColumnInfo for a complex column.
     */
    public static ComplexColumnInfo<? extends ComplexValue> create(ColumnImpl column, ByteBuffer buffer, int offset) throws IOException {
        int complexTypeId = buffer.getInt(offset + column.getFormat().OFFSET_COLUMN_COMPLEX_ID);

        DatabaseImpl db = column.getDatabase(); // NOPMD CloseResource - borrowed reference, lifecycle owned by the caller/Database itself
        TableImpl complexColumns = db.getSystemComplexColumns();
        IndexCursor cursor = CursorBuilder.createCursor(complexColumns.getPrimaryKeyIndex());
        if (!cursor.findFirstRowByEntry(complexTypeId)) {
            throw new IOException(column.withErrorContext("Could not find complex column info for complex column with id " + complexTypeId));
        }
        Row cColRow = cursor.getCurrentRow();
        int tableId = cColRow.getInt(COL_TABLE_ID);
        if (tableId != column.getTable().getTableDefPageNumber()) {
            throw new IOException(column.withErrorContext("Found complex column for table " + tableId + " but expected table " + column.getTable().getTableDefPageNumber()));
        }
        String colName = cColRow.getString(COL_COLUMN_NAME);
        if (colName != null && !colName.equalsIgnoreCase(column.getName())) {
            throw new IOException(column.withErrorContext(String.format("Found complex column info for column %s but expected column %s", colName, column.getName())));
        }
        int flatTableId = cColRow.getInt(COL_FLAT_TABLE_ID);
        int typeObjId = cColRow.getInt(COL_COMPLEX_TYPE_OBJECT_ID);

        TableImpl typeObjTable = db.getTable(typeObjId);
        TableImpl flatTable = db.getTable(flatTableId);

        if (typeObjTable == null || flatTable == null) {
            throw new IOException(column.withErrorContext("Could not find supporting tables (" + typeObjId + ", " + flatTableId + ") for complex column with id " + complexTypeId));
        }

        // access reserves the name of every "type table", so the name says which kind of complex column this is. The
        // attachment table has to be matched before the general prefix, which it also starts with
        String typeName = typeObjTable.getName();

        if (ATTACHMENT_TYPE_TABLE.equalsIgnoreCase(typeName)) {
            return new AttachmentColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        } else if (StringUtil.startsWithIgnoreCase(typeName, VERSION_HISTORY_TYPE_PREFIX)) {
            return new VersionHistoryColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        } else if (StringUtil.startsWithIgnoreCase(typeName, COMPLEX_TYPE_PREFIX)) {
            // the name says multi-value, but the value still has to be of a type we can read
            if (isMultiValueColumn(typeObjTable)) {
                return new MultiValueColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
            }
            LOGGER.log(Level.WARNING, () -> column.withErrorContext(String.format("Unsupported multi-value column type %s", typeName)));
            return new UnsupportedColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        }

        // the name is not one we know, so fall back to the shape of the type table
        if (isMultiValueColumn(typeObjTable)) {
            return new MultiValueColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        } else if (isAttachmentColumn(typeObjTable)) {
            return new AttachmentColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        } else if (isVersionHistoryColumn(typeObjTable)) {
            return new VersionHistoryColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
        }

        LOGGER.log(Level.WARNING, column.withErrorContext("Unsupported complex column type " + typeObjTable.getName()));
        return new UnsupportedColumnInfoImpl(column, complexTypeId, typeObjTable, flatTable);
    }

    public static boolean isMultiValueColumn(Table typeObjTable) {
        // if we found a single value of a "simple" type, then we are dealing with
        // a multi-value column
        List<? extends Column> typeCols = typeObjTable.getColumns();
        return typeCols.size() == 1 && MULTI_VALUE_TYPES.contains(typeCols.get(0).getType());
    }

    public static boolean isAttachmentColumn(Table typeObjTable) {
        // attachment data has these columns FileURL(MEMO), FileName(TEXT),
        // FileType(TEXT), FileData(OLE), FileTimeStamp(SHORT_DATE_TIME),
        // FileFlags(LONG)
        List<? extends Column> typeCols = typeObjTable.getColumns();
        if (typeCols.size() < 6) {
            return false;
        }

        int numMemo = 0;
        int numText = 0;
        int numDate = 0;
        int numOle = 0;
        int numLong = 0;

        for (Column col : typeCols) {
            switch (col.getType()) {
                case TEXT:
                    numText++;
                    break;
                case LONG:
                    numLong++;
                    break;
                case SHORT_DATE_TIME:
                    numDate++;
                    break;
                case OLE:
                    numOle++;
                    break;
                case MEMO:
                    numMemo++;
                    break;
                default:
                    // ignore
            }
        }

        // be flexible, allow for extra columns...
        return numMemo >= 1 && numText >= 2 && numOle >= 1 && numDate >= 1 && numLong >= 1;
    }

    public static boolean isVersionHistoryColumn(Table typeObjTable) {
        // version history data has these columns <value>(MEMO),
        // <modified>(SHORT_DATE_TIME)
        List<? extends Column> typeCols = typeObjTable.getColumns();
        if (typeCols.size() < 2) {
            return false;
        }

        int numMemo = 0;
        int numDate = 0;

        for (Column col : typeCols) {
            switch (col.getType()) {
                case SHORT_DATE_TIME:
                    numDate++;
                    break;
                case MEMO:
                    numMemo++;
                    break;
                default:
                    // ignore
            }
        }

        // be flexible, allow for extra columns...
        return numMemo >= 1 && numDate >= 1;
    }
}
