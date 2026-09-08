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

import io.github.spannm.jackcess.*;

import java.io.IOException;
import java.util.*;

/**
 * Helper class used to maintain state during table creation.
 */
public class TableCreator extends TableMutator {
    private String                                name;
    private List<ColumnBuilder>                   columns;
    private List<IndexBuilder>                    indexes;
    private final List<IndexDataState>            indexDataStates = new ArrayList<>();
    private final Map<ColumnBuilder, ColumnState> columnStates    = new IdentityHashMap<>();
    private final List<ColumnBuilder>             lvalCols        = new ArrayList<>();
    private int                                   tdefPageNumber  = PageChannel.INVALID_PAGE_NUMBER;
    private int                                   umapPageNumber  = PageChannel.INVALID_PAGE_NUMBER;
    private int                                   indexCount;
    private int                                   logicalIndexCount;

    public TableCreator(DatabaseImpl database) {
        super(database);
    }

    public String getName() {
        return name;
    }

    @Override
    String getTableName() {
        return getName();
    }

    @Override
    public int getTdefPageNumber() {
        return tdefPageNumber;
    }

    public int getUmapPageNumber() {
        return umapPageNumber;
    }

    public List<ColumnBuilder> getColumns() {
        return columns;
    }

    public List<IndexBuilder> getIndexes() {
        return indexes;
    }

    public boolean hasIndexes() {
        return !indexes.isEmpty();
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getLogicalIndexCount() {
        return logicalIndexCount;
    }

    @Override
    public IndexDataState getIndexDataState(IndexBuilder idx) {
        for (IndexDataState idxDataState : indexDataStates) {
            for (IndexBuilder curIdx : idxDataState.getIndexes()) {
                if (idx == curIdx) {
                    return idxDataState;
                }
            }
        }
        throw new IllegalStateException(withErrorContext("could not find state for index"));
    }

    public List<IndexDataState> getIndexDataStates() {
        return indexDataStates;
    }

    @Override
    public ColumnState getColumnState(ColumnBuilder col) {
        return columnStates.get(col);
    }

    public List<ColumnBuilder> getLongValueColumns() {
        return lvalCols;
    }

    @Override
    short getColumnNumber(String colName) {
        for (ColumnBuilder col : columns) {
            if (col.getName().equalsIgnoreCase(colName)) {
                return col.getColumnNumber();
            }
        }
        return IndexData.COLUMN_UNUSED;
    }

    /**
     * @return The number of variable length columns which are not long values found in the list
     */
    public short countNonLongVariableLength() {
        short rtn = 0;
        for (ColumnBuilder col : columns) {
            if (col.isVariableLength() && !col.getType().isLongValue()) {
                rtn++;
            }
        }
        return rtn;
    }

    /**
     * Creates the table in the database.
     */
    public TableImpl createTable(TableBuilder table) throws IOException {

        name = table.getName();
        columns = table.getColumns();
        indexes = table.getIndexes();
        if (indexes == null) {
            indexes = List.of();
        }

        validate();

        // assign column numbers and do some assorted column bookkeeping
        short columnNumber = (short) 0;
        for (ColumnBuilder col : columns) {
            col.setColumnNumber(columnNumber++);
            if (col.getType().isLongValue()) {
                lvalCols.add(col);
                // only lval columns need extra state
                columnStates.put(col, new ColumnState());
            }
        }

        if (hasIndexes()) {
            // sort out index numbers (and backing index data).
            for (IndexBuilder idx : indexes) {
                idx.setIndexNumber(logicalIndexCount++);
                findIndexDataState(idx);
            }
        }

        getPageChannel().startExclusiveWrite();
        try {

            // reserve some pages
            tdefPageNumber = reservePageNumber();
            umapPageNumber = reservePageNumber();

            // Write the tdef page to disk.
            TableImpl.writeTableDefinition(this);

            // update the database with the new table info
            getDatabase().addNewTable(name, tdefPageNumber, DatabaseImpl.TYPE_TABLE, null, null);

            TableImpl newTable = getDatabase().getTable(name);

            // add any table properties
            boolean addedProps = false;
            Map<String, PropertyMap.Property> props = table.getProperties();
            if (props != null) {
                newTable.getProperties().putAll(props.values());
                addedProps = true;
            }
            for (ColumnBuilder cb : columns) {
                Map<String, PropertyMap.Property> colProps = cb.getProperties();
                if (colProps != null) {
                    newTable.getColumn(cb.getName()).getProperties().putAll(colProps.values());
                    addedProps = true;
                }
            }

            // all table and column props are saved together
            if (addedProps) {
                newTable.getProperties().save();
            }

            return newTable;

        } finally {
            getPageChannel().finishWrite();
        }
    }

    private IndexDataState findIndexDataState(IndexBuilder idx) {

        // search for an index which matches the given index (in terms of the
        // backing data)
        for (IndexDataState idxDataState : indexDataStates) {
            if (sameIndexData(idxDataState.getFirstIndex(), idx)) {
                idxDataState.addIndex(idx);
                return idxDataState;
            }
        }

        // no matches found, need new index data state
        IndexDataState idxDataState = new IndexDataState();
        idxDataState.setIndexDataNumber(indexCount++);
        idxDataState.addIndex(idx);
        indexDataStates.add(idxDataState);
        return idxDataState;
    }

    /**
     * Validates the new table information before attempting creation.
     */
    private void validate() throws IOException {

        getDatabase().validateNewTableName(name);

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(withErrorContext("Cannot create table with no columns"));
        }
        if (columns.size() > getFormat().MAX_COLUMNS_PER_TABLE) {
            throw new IllegalArgumentException(withErrorContext("Cannot create table with more than " + getFormat().MAX_COLUMNS_PER_TABLE + " columns"));
        }

        Set<String> colNames = new HashSet<>();
        // next, validate the column definitions
        for (ColumnBuilder column : columns) {
            validateColumn(colNames, column);
        }

        List<ColumnBuilder> autoCols = getAutoNumberColumns();
        if (autoCols.size() > 1) {
            // for most autonumber types, we can only have one of each type
            Set<DataType> autoTypes = EnumSet.noneOf(DataType.class);
            for (ColumnBuilder c : autoCols) {
                validateAutoNumberColumn(autoTypes, c);
            }
        }

        if (hasIndexes()) {

            if (indexes.size() > getFormat().MAX_INDEXES_PER_TABLE) {
                throw new IllegalArgumentException(withErrorContext("Cannot create table with more than " + getFormat().MAX_INDEXES_PER_TABLE + " indexes"));
            }

            // now, validate the indexes
            Set<String> idxNames = new HashSet<>();
            boolean[] foundPk = new boolean[1];
            for (IndexBuilder index : indexes) {
                validateIndex(colNames, idxNames, foundPk, index);
            }
        }
    }

    private List<ColumnBuilder> getAutoNumberColumns() {
        List<ColumnBuilder> autoCols = new ArrayList<>(1);
        for (ColumnBuilder c : columns) {
            if (c.isAutoNumber()) {
                autoCols.add(c);
            }
        }
        return autoCols;
    }

    private static boolean sameIndexData(IndexBuilder idx1, IndexBuilder idx2) {
        // index data can be combined if flags match and columns (and col flags)
        // match
        if ((idx1.getFlags() != idx2.getFlags()) || (idx1.getColumns().size() != idx2.getColumns().size())) {
            return false;
        }

        for (int i = 0; i < idx1.getColumns().size(); ++i) {
            IndexBuilder.Column col1 = idx1.getColumns().get(i);
            IndexBuilder.Column col2 = idx2.getColumns().get(i);

            if (!sameIndexData(col1, col2)) {
                return false;
            }
        }

        return true;
    }

    private static boolean sameIndexData(IndexBuilder.Column col1, IndexBuilder.Column col2) {
        return col1.getName().equals(col2.getName()) && col1.getFlags() == col2.getFlags();
    }

    @Override
    protected String withErrorContext(String msg) {
        return msg + "(Table=" + getName() + ")";
    }
}
