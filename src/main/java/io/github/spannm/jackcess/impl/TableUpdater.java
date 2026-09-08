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

import io.github.spannm.jackcess.ColumnBuilder;
import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.IndexBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Helper class used to maintain state during table mutation.
 */
public class TableUpdater extends TableMutator {
    private final TableImpl               table;

    private ColumnBuilder                 column;
    private IndexBuilder                  index;
    private int                           origTdefLen;
    private int                           addedTdefLen;
    private final List<Integer>           nextPages = new ArrayList<>(1);
    private ColumnState                   colState;
    private IndexDataState                idxDataState;
    private IndexImpl.ForeignKeyReference fkReference;

    public TableUpdater(TableImpl table) {
        super(table.getDatabase());
        this.table = table;
    }

    public ColumnBuilder getColumn() {
        return column;
    }

    public IndexBuilder getIndex() {
        return index;
    }

    @Override
    String getTableName() {
        return table.getName();
    }

    @Override
    public int getTdefPageNumber() {
        return table.getTableDefPageNumber();
    }

    @Override
    short getColumnNumber(String colName) {
        for (ColumnImpl col : table.getColumns()) {
            if (col.getName().equalsIgnoreCase(colName)) {
                return col.getColumnNumber();
            }
        }
        return IndexData.COLUMN_UNUSED;
    }

    @Override
    public ColumnState getColumnState(ColumnBuilder col) {
        return col == column ? colState : null;
    }

    @Override
    public IndexDataState getIndexDataState(IndexBuilder idx) {
        return idx == index ? idxDataState : null;
    }

    void setForeignKey(IndexImpl.ForeignKeyReference newFkReference) {
        fkReference = newFkReference;
    }

    @Override
    public IndexImpl.ForeignKeyReference getForeignKey(IndexBuilder idx) {
        return idx == index ? fkReference : null;
    }

    int getAddedTdefLen() {
        return addedTdefLen;
    }

    void addTdefLen(int add) {
        addedTdefLen += add;
    }

    void setOrigTdefLen(int len) {
        origTdefLen = len;
    }

    List<Integer> getNextPages() {
        return nextPages;
    }

    void resetTdefInfo() {
        addedTdefLen = 0;
        origTdefLen = 0;
        nextPages.clear();
    }

    public ColumnImpl addColumn(ColumnBuilder newColumn) throws IOException {

        column = newColumn;

        validateAddColumn();

        // assign column number and do some assorted column bookkeeping
        short columnNumber = (short) table.getMaxColumnCount();
        this.column.setColumnNumber(columnNumber);
        if (this.column.getType().isLongValue()) {
            colState = new ColumnState();
        }

        getPageChannel().startExclusiveWrite();
        try {

            return table.mutateAddColumn(this);

        } finally {
            getPageChannel().finishWrite();
        }
    }

    public IndexImpl addIndex(IndexBuilder newIndex) throws IOException {
        return addIndex(newIndex, false, (byte) 0, (byte) 0);
    }

    IndexImpl addIndex(IndexBuilder newIndex, boolean isInternal, byte ignoreIdxFlags, byte ignoreColFlags) throws IOException {
        index = newIndex;

        if (!isInternal) {
            validateAddIndex();
        }

        // assign index number and do some assorted index bookkeeping
        int indexNumber = table.getLogicalIndexCount();
        this.index.setIndexNumber(indexNumber);

        // initialize backing index state
        initIndexDataState(ignoreIdxFlags, ignoreColFlags);

        if (!isInternal) {
            getPageChannel().startExclusiveWrite();
        } else {
            // if "internal" update, this is part of a larger operation which
            // already holds an exclusive write lock
            getPageChannel().startWrite();
        }
        try {

            if (idxDataState.getIndexDataNumber() == table.getIndexCount()) {
                // we need a new backing index data
                table.mutateAddIndexData(this);

                // we need to modify the table def again when adding the Index, so reset
                resetTdefInfo();
            }

            return table.mutateAddIndex(this);

        } finally {
            getPageChannel().finishWrite();
        }
    }

    boolean validateUpdatedTdef(ByteBuffer tableBuffer) {
        // sanity check the updates
        return origTdefLen + addedTdefLen == tableBuffer.limit();
    }

    private void validateAddColumn() {

        if (column == null) {
            throw new IllegalArgumentException(withErrorContext("Cannot add column with no column"));
        }
        if (table.getColumnCount() + 1 > getFormat().MAX_COLUMNS_PER_TABLE) {
            throw new IllegalArgumentException(withErrorContext("Cannot add column to table with " + getFormat().MAX_COLUMNS_PER_TABLE + " columns"));
        }

        Set<String> colNames = getColumnNames();
        // next, validate the column definition
        validateColumn(colNames, column);

        if (column.isAutoNumber()) {
            // for most autonumber types, we can only have one of each type
            Set<DataType> autoTypes = EnumSet.noneOf(DataType.class);
            for (ColumnImpl autoCol : table.getAutoNumberColumns()) {
                autoTypes.add(autoCol.getType());
            }

            validateAutoNumberColumn(autoTypes, column);
        }
    }

    private void validateAddIndex() {

        if (index == null) {
            throw new IllegalArgumentException(withErrorContext("Cannot add index with no index"));
        }
        if (table.getLogicalIndexCount() + 1 > getFormat().MAX_INDEXES_PER_TABLE) {
            throw new IllegalArgumentException(withErrorContext("Cannot add index to table with " + getFormat().MAX_INDEXES_PER_TABLE + " indexes"));
        }

        boolean[] foundPk = new boolean[1];
        Set<String> idxNames = getIndexNames(table, foundPk);
        // next, validate the index definition
        validateIndex(getColumnNames(), idxNames, foundPk, index);
    }

    private Set<String> getColumnNames() {
        Set<String> colNames = new HashSet<>();
        for (ColumnImpl col : table.getColumns()) {
            colNames.add(DatabaseImpl.toLookupName(col.getName()));
        }
        return colNames;
    }

    static Set<String> getIndexNames(TableImpl table, boolean[] foundPk) {
        Set<String> idxNames = new HashSet<>();
        for (IndexImpl index : table.getIndexes()) {
            idxNames.add(DatabaseImpl.toLookupName(index.getName()));
            if (index.isPrimaryKey() && foundPk != null) {
                foundPk[0] = true;
            }
        }
        return idxNames;
    }

    private void initIndexDataState(byte ignoreIdxFlags, byte ignoreColFlags) {

        idxDataState = new IndexDataState();
        idxDataState.addIndex(index);

        // search for an existing index which matches the given index (in terms of
        // the backing data)
        IndexData idxData = findIndexData(index, table, ignoreIdxFlags, ignoreColFlags);

        int idxDataNumber = idxData != null ? idxData.getIndexDataNumber() : table.getIndexCount();

        idxDataState.setIndexDataNumber(idxDataNumber);
    }

    static IndexData findIndexData(IndexBuilder idx, TableImpl table, byte ignoreIdxFlags, byte ignoreColFlags) {
        for (IndexData idxData : table.getIndexDatas()) {
            if (sameIndexData(idx, idxData, ignoreIdxFlags, ignoreColFlags)) {
                return idxData;
            }
        }
        return null;
    }

    private static boolean sameIndexData(IndexBuilder idx1, IndexData idx2, byte ignoreIdxFlags, byte ignoreColFlags) {
        // index data can be combined if flags match and columns (and col flags)
        // match
        if (((idx1.getFlags() | ignoreIdxFlags) != (idx2.getIndexFlags() | ignoreIdxFlags)) || (idx1.getColumns().size() != idx2.getColumnCount())) {
            return false;
        }

        for (int i = 0; i < idx1.getColumns().size(); ++i) {
            IndexBuilder.Column col1 = idx1.getColumns().get(i);
            IndexData.ColumnDescriptor col2 = idx2.getColumns().get(i);

            if (!sameIndexData(col1, col2, ignoreColFlags)) {
                return false;
            }
        }

        return true;
    }

    private static boolean sameIndexData(IndexBuilder.Column col1, IndexData.ColumnDescriptor col2, int ignoreColFlags) {
        return col1.getName().equals(col2.getName()) && (col1.getFlags() | ignoreColFlags) == (col2.getFlags() | ignoreColFlags);
    }

    @Override
    protected String withErrorContext(String msg) {
        String objStr = "";
        if (column != null) {
            objStr = ";Column=" + column.getName();
        } else if (index != null) {
            objStr = ";Index=" + index.getName();
        }
        return msg + "(Table=" + table.getName() + objStr + ")";
    }
}
