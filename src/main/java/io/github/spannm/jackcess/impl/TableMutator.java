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
    private ColumnOffsets _colOffsets;

    protected TableMutator(DatabaseImpl database) {
        super(database);
    }

    public void setColumnOffsets(int fixedOffset, int varOffset, int longVarOffset) {
        if (_colOffsets == null) {
            _colOffsets = new ColumnOffsets();
        }
        _colOffsets.set(fixedOffset, varOffset, longVarOffset);
    }

    public ColumnOffsets getColumnOffsets() {
        return _colOffsets;
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
        private short _fixedOffset;
        private short _varOffset;
        private short _longVarOffset;

        public void set(int fixedOffset, int varOffset, int longVarOffset) {
            _fixedOffset = (short) fixedOffset;
            _varOffset = (short) varOffset;
            _longVarOffset = (short) longVarOffset;
        }

        public short getNextVariableOffset(ColumnBuilder col) {
            if (!col.isVariableLength()) {
                return _varOffset;
            }
            if (!col.getType().isLongValue()) {
                return _varOffset++;
            }
            return _longVarOffset++;
        }

        public short getNextFixedOffset(ColumnBuilder col) {
            if (col.storeInNullMask()) {
                // booleans are stored in null mask, not in fixed data section
                return 0;
            }
            short offset = _fixedOffset;
            _fixedOffset += col.getFixedDataSize();
            return offset;
        }
    }

    /**
     * Maintains additional state used during column creation.
     */
    static final class ColumnState {
        private byte _umapOwnedRowNumber;
        private byte _umapFreeRowNumber;
        // we always put both usage maps on the same page
        private int  _umapPageNumber;

        public byte getUmapOwnedRowNumber() {
            return _umapOwnedRowNumber;
        }

        public void setUmapOwnedRowNumber(byte newUmapOwnedRowNumber) {
            _umapOwnedRowNumber = newUmapOwnedRowNumber;
        }

        public byte getUmapFreeRowNumber() {
            return _umapFreeRowNumber;
        }

        public void setUmapFreeRowNumber(byte newUmapFreeRowNumber) {
            _umapFreeRowNumber = newUmapFreeRowNumber;
        }

        public int getUmapPageNumber() {
            return _umapPageNumber;
        }

        public void setUmapPageNumber(int newUmapPageNumber) {
            _umapPageNumber = newUmapPageNumber;
        }
    }

    /**
     * Maintains additional state used during index data creation.
     */
    static final class IndexDataState {
        private final List<IndexBuilder> _indexes = new ArrayList<>();
        private int                      _indexDataNumber;
        private byte                     _umapRowNumber;
        private int                      _umapPageNumber;
        private int                      _rootPageNumber;

        public IndexBuilder getFirstIndex() {
            // all indexes which have the same backing IndexDataState will have
            // equivalent columns and flags.
            return _indexes.get(0);
        }

        public List<IndexBuilder> getIndexes() {
            return _indexes;
        }

        public void addIndex(IndexBuilder idx) {
            _indexes.add(idx);
        }

        public int getIndexDataNumber() {
            return _indexDataNumber;
        }

        public void setIndexDataNumber(int newIndexDataNumber) {
            _indexDataNumber = newIndexDataNumber;
        }

        public byte getUmapRowNumber() {
            return _umapRowNumber;
        }

        public void setUmapRowNumber(byte newUmapRowNumber) {
            _umapRowNumber = newUmapRowNumber;
        }

        public int getUmapPageNumber() {
            return _umapPageNumber;
        }

        public void setUmapPageNumber(int newUmapPageNumber) {
            _umapPageNumber = newUmapPageNumber;
        }

        public int getRootPageNumber() {
            return _rootPageNumber;
        }

        public void setRootPageNumber(int newRootPageNumber) {
            _rootPageNumber = newRootPageNumber;
        }
    }
}
