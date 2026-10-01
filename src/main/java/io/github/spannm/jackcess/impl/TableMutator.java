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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Common helper class used to maintain state during table mutation.
 */
public abstract class TableMutator extends DBMutator {
    private ColumnOffsets colOffsets;

    protected TableMutator(DatabaseImpl database) {
        super(database);
    }

    public void setColumnOffsets(int fixedOffset, int varOffset, int longVarOffset) {
        if (colOffsets == null) {
            colOffsets = new ColumnOffsets();
        }
        colOffsets.set(fixedOffset, varOffset, longVarOffset);
    }

    public ColumnOffsets getColumnOffsets() {
        return colOffsets;
    }

    public IndexImpl.ForeignKeyReference getForeignKey(IndexBuilder idx) {
        return null;
    }

    protected void validateColumn(Set<String> colNames, ColumnBuilder column) {

        // FIXME for now, we can't create complex columns
        if (column.getType() == DataType.COMPLEX_TYPE) {
            throw new UnsupportedOperationException(withErrorContext("Complex column creation is not yet implemented"));
        }

        column.validate(getFormat());
        if (!colNames.add(DatabaseImpl.toLookupName(column.getName()))) {
            throw new IllegalArgumentException(withErrorContext("duplicate column name: " + column.getName()));
        }

        setColumnSortOrder(column);
    }

    protected void validateIndex(Set<String> colNames, Set<String> idxNames, boolean[] foundPk, IndexBuilder index) {

        index.validate(colNames, getFormat());
        if (!idxNames.add(DatabaseImpl.toLookupName(index.getName()))) {
            throw new IllegalArgumentException(withErrorContext("duplicate index name: " + index.getName()));
        }
        if (index.isPrimaryKey()) {
            if (foundPk[0]) {
                throw new IllegalArgumentException(withErrorContext("found second primary key index: " + index.getName()));
            }
            foundPk[0] = true;
        } else if (index.getType() == IndexImpl.FOREIGN_KEY_INDEX_TYPE) {
            if (getForeignKey(index) == null) {
                throw new IllegalArgumentException(withErrorContext("missing foreign key info for " + index.getName()));
            }
        }
    }

    protected void validateAutoNumberColumn(Set<DataType> autoTypes, ColumnBuilder column) {
        if (!column.getType().isMultipleAutoNumberAllowed() && !autoTypes.add(column.getType())) {
            throw new IllegalArgumentException(withErrorContext("Can have at most one AutoNumber column of type " + column.getType() + " per table"));
        }
    }

    private void setColumnSortOrder(ColumnBuilder column) {
        // set the sort order to the db default (if unspecified)
        if (column.getType().isTextual() && column.getTextSortOrder() == null) {
            column.setTextSortOrder(getDbSortOrder());
        }
    }

    abstract String getTableName();

    public abstract int getTdefPageNumber();

    abstract short getColumnNumber(String colName);

    public abstract ColumnState getColumnState(ColumnBuilder col);

    public abstract IndexDataState getIndexDataState(IndexBuilder idx);

    protected abstract String withErrorContext(String msg);

    /**
     * Maintains additional state used during column writing.
     */
    static final class ColumnOffsets {
        private short fixedOffset;
        private short varOffset;
        private short longVarOffset;

        public void set(int newFixedOffset, int newVarOffset, int newLongVarOffset) {
            fixedOffset = (short) newFixedOffset;
            varOffset = (short) newVarOffset;
            longVarOffset = (short) newLongVarOffset;
        }

        public short getNextVariableOffset(ColumnBuilder col) {
            if (!col.isVariableLength()) {
                return varOffset;
            }
            if (!col.getType().isLongValue()) {
                return varOffset++;
            }
            return longVarOffset++;
        }

        public short getNextFixedOffset(ColumnBuilder col) {
            if (col.storeInNullMask()) {
                // booleans are stored in null mask, not in fixed data section
                return 0;
            }
            short offset = fixedOffset;
            fixedOffset += col.getFixedDataSize();
            return offset;
        }
    }

    /**
     * Maintains additional state used during column creation.
     */
    static final class ColumnState {
        private byte umapOwnedRowNumber;
        private byte umapFreeRowNumber;
        // we always put both usage maps on the same page
        private int  umapPageNumber;

        public byte getUmapOwnedRowNumber() {
            return umapOwnedRowNumber;
        }

        public void setUmapOwnedRowNumber(byte newUmapOwnedRowNumber) {
            umapOwnedRowNumber = newUmapOwnedRowNumber;
        }

        public byte getUmapFreeRowNumber() {
            return umapFreeRowNumber;
        }

        public void setUmapFreeRowNumber(byte newUmapFreeRowNumber) {
            umapFreeRowNumber = newUmapFreeRowNumber;
        }

        public int getUmapPageNumber() {
            return umapPageNumber;
        }

        public void setUmapPageNumber(int newUmapPageNumber) {
            umapPageNumber = newUmapPageNumber;
        }
    }

    /**
     * Maintains additional state used during index data creation.
     */
    static final class IndexDataState {
        private final List<IndexBuilder> indexes = new ArrayList<>();
        private int                      indexDataNumber;
        private byte                     umapRowNumber;
        private int                      umapPageNumber;
        private int                      rootPageNumber;

        public IndexBuilder getFirstIndex() {
            // all indexes which have the same backing IndexDataState will have
            // equivalent columns and flags.
            return indexes.get(0);
        }

        public List<IndexBuilder> getIndexes() {
            return indexes;
        }

        public void addIndex(IndexBuilder idx) {
            indexes.add(idx);
        }

        public int getIndexDataNumber() {
            return indexDataNumber;
        }

        public void setIndexDataNumber(int newIndexDataNumber) {
            indexDataNumber = newIndexDataNumber;
        }

        public byte getUmapRowNumber() {
            return umapRowNumber;
        }

        public void setUmapRowNumber(byte newUmapRowNumber) {
            umapRowNumber = newUmapRowNumber;
        }

        public int getUmapPageNumber() {
            return umapPageNumber;
        }

        public void setUmapPageNumber(int newUmapPageNumber) {
            umapPageNumber = newUmapPageNumber;
        }

        public int getRootPageNumber() {
            return rootPageNumber;
        }

        public void setRootPageNumber(int newRootPageNumber) {
            rootPageNumber = newRootPageNumber;
        }
    }
}
