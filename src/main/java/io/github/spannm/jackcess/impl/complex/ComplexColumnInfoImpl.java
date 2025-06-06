package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.complex.ComplexColumnInfo;
import io.github.spannm.jackcess.complex.ComplexDataType;
import io.github.spannm.jackcess.complex.ComplexValue;
import io.github.spannm.jackcess.complex.ComplexValueForeignKey;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.util.ToStringBuilder;

import java.io.IOException;
import java.util.*;

/**
 * Base class for the additional information tracked for complex columns.
 */
public abstract class ComplexColumnInfoImpl<V extends ComplexValue> implements ComplexColumnInfo<V> {
    private static final int                   INVALID_ID_VALUE = -1;
    public static final ComplexValue.Id        INVALID_ID       = new ComplexValueIdImpl(INVALID_ID_VALUE, null);
    public static final ComplexValueForeignKey INVALID_FK       = new ComplexValueForeignKeyImpl(null, INVALID_ID_VALUE);

    private final Column                       mcolumn;
    private final int                          mcomplexTypeId;
    private final Table                        mflatTable;
    private final List<Column>                 mtypeCols;
    private final Column                       mpkCol;
    private final Column                       mcomplexValFkCol;
    private IndexCursor                        momplexValIdCursor;

    protected ComplexColumnInfoImpl(Column column, int complexTypeId, Table typeObjTable, Table flatTable) throws IOException {
        mcolumn = column;
        mcomplexTypeId = complexTypeId;
        mflatTable = flatTable;

        // the flat table has all the "value" columns and 2 extra columns, a
        // primary key for each row, and a LONG value which is essentially a
        // foreign key to the main table.
        List<Column> typeCols = new ArrayList<>();
        List<Column> otherCols = new ArrayList<>();
        diffFlatColumns(typeObjTable, flatTable, typeCols, otherCols);

        mtypeCols = Collections.unmodifiableList(typeCols);

        Column pkCol = null;
        Column complexValFkCol = null;
        for (Column col : otherCols) {
            if (col.isAutoNumber()) {
                pkCol = col;
            } else if (col.getType() == DataType.LONG) {
                complexValFkCol = col;
            }
        }

        if (pkCol == null || complexValFkCol == null) {
            throw new IOException("Could not find expected columns in flat table " + flatTable.getName() + " for complex column with id " + complexTypeId);
        }
        mpkCol = pkCol;
        mcomplexValFkCol = complexValFkCol;
    }

    public void postTableLoadInit() throws IOException {
        // nothing to do in base class
    }

    public Column getColumn() {
        return mcolumn;
    }

    public Database getDatabase() {
        return getColumn().getDatabase();
    }

    public Column getPrimaryKeyColumn() {
        return mpkCol;
    }

    public Column getComplexValueForeignKeyColumn() {
        return mcomplexValFkCol;
    }

    protected List<Column> getTypeColumns() {
        return mtypeCols;
    }

    @Override
    public int countValues(int complexValueFk) throws IOException {
        return getRawValues(complexValueFk, Collections.singleton(mcomplexValFkCol.getName())).size();
    }

    @Override
    public List<Row> getRawValues(int complexValueFk) throws IOException {
        return getRawValues(complexValueFk, null);
    }

    private Iterator<Row> getComplexValFkIter(int complexValueFk, Collection<String> columnNames) throws IOException {
        if (momplexValIdCursor == null) {
            momplexValIdCursor = mflatTable.newCursor().withIndexByColumns(mcomplexValFkCol).toIndexCursor();
        }

        return momplexValIdCursor.newEntryIterable(complexValueFk).withColumnNames(columnNames).iterator();
    }

    @Override
    public List<Row> getRawValues(int complexValueFk, Collection<String> columnNames) throws IOException {
        Iterator<Row> entryIter = getComplexValFkIter(complexValueFk, columnNames);
        if (!entryIter.hasNext()) {
            return List.of();
        }

        List<Row> values = new ArrayList<>();
        while (entryIter.hasNext()) {
            values.add(entryIter.next());
        }

        return values;
    }

    @Override
    public List<V> getValues(ComplexValueForeignKey complexValueFk) throws IOException {
        List<Row> rawValues = getRawValues(complexValueFk.get());
        if (rawValues.isEmpty()) {
            return List.of();
        }

        return toValues(complexValueFk, rawValues);
    }

    protected List<V> toValues(ComplexValueForeignKey complexValueFk, List<Row> rawValues) throws IOException {
        List<V> values = new ArrayList<>();
        for (Row rawValue : rawValues) {
            values.add(toValue(complexValueFk, rawValue));
        }

        return values;
    }

    @Override
    public ComplexValue.Id addRawValue(Map<String, ?> rawValue) throws IOException {
        Object[] row = ((TableImpl) mflatTable).asRowWithRowId(rawValue);
        mflatTable.addRow(row);
        return getValueId(row);
    }

    @Override
    public ComplexValue.Id addValue(V value) throws IOException {
        Object[] row = asRow(newRowArray(), value);
        mflatTable.addRow(row);
        ComplexValue.Id id = getValueId(row);
        value.setId(id);
        return id;
    }

    @Override
    public void addValues(Collection<? extends V> values) throws IOException {
        for (V value : values) {
            addValue(value);
        }
    }

    @Override
    public ComplexValue.Id updateRawValue(Row rawValue) throws IOException {
        mflatTable.updateRow(rawValue);
        return getValueId(rawValue);
    }

    @Override
    public ComplexValue.Id updateValue(V value) throws IOException {
        ComplexValue.Id id = value.getId();
        updateRow(id, asRow(newRowArray(), value));
        return id;
    }

    @Override
    public void updateValues(Collection<? extends V> values) throws IOException {
        for (V value : values) {
            updateValue(value);
        }
    }

    @Override
    public void deleteRawValue(Row rawValue) throws IOException {
        deleteRow(rawValue.getId());
    }

    @Override
    public void deleteValue(V value) throws IOException {
        deleteRow(value.getId().getRowId());
    }

    @Override
    public void deleteValues(Collection<? extends V> values) throws IOException {
        for (V value : values) {
            deleteValue(value);
        }
    }

    @Override
    public void deleteAllValues(int complexValueFk) throws IOException {
        Iterator<Row> entryIter = getComplexValFkIter(complexValueFk, Set.of());
        while (entryIter.hasNext()) {
            entryIter.next();
            entryIter.remove();
        }
    }

    @Override
    public void deleteAllValues(ComplexValueForeignKey complexValueFk) throws IOException {
        deleteAllValues(complexValueFk.get());
    }

    private void updateRow(ComplexValue.Id id, Object[] row) throws IOException {
        ((TableImpl) mflatTable).updateRow(id.getRowId(), row);
    }

    private void deleteRow(RowId rowId) throws IOException {
        ((TableImpl) mflatTable).deleteRow(rowId);
    }

    protected ComplexValueIdImpl getValueId(Row row) {
        int idVal = (Integer) getPrimaryKeyColumn().getRowValue(row);
        return new ComplexValueIdImpl(idVal, row.getId());
    }

    protected ComplexValueIdImpl getValueId(Object[] row) {
        int idVal = (Integer) getPrimaryKeyColumn().getRowValue(row);
        return new ComplexValueIdImpl(idVal, ((TableImpl) mflatTable).getRowId(row));
    }

    protected Object[] asRow(Object[] row, V value) throws IOException {
        ComplexValue.Id id = value.getId();
        mpkCol.setRowValue(row, id != INVALID_ID ? id : Column.AUTO_NUMBER);
        ComplexValueForeignKey cFk = value.getComplexValueForeignKey();
        mcomplexValFkCol.setRowValue(row, cFk != INVALID_FK ? cFk : Column.AUTO_NUMBER);
        return row;
    }

    private Object[] newRowArray() {
        Object[] row = new Object[mflatTable.getColumnCount() + 1];
        row[row.length - 1] = ColumnImpl.RETURN_ROW_ID;
        return row;
    }

    @Override
    public String toString() {
        return ToStringBuilder.valueBuilder(this).append("complexType", getType()).append("complexTypeId", mcomplexTypeId).toString();
    }

    protected static void diffFlatColumns(Table typeObjTable, Table flatTable, List<Column> typeCols, List<Column> otherCols) {
        // each "flat"" table has the columns from the "type" table, plus some
        // others. separate the "flat" columns into these 2 buckets
        for (Column col : flatTable.getColumns()) {
            if (((TableImpl) typeObjTable).hasColumn(col.getName())) {
                typeCols.add(col);
            } else {
                otherCols.add(col);
            }
        }
    }

    @Override
    public abstract ComplexDataType getType();

    protected abstract V toValue(ComplexValueForeignKey complexValueFk, Row rawValues);

    protected abstract static class ComplexValueImpl implements ComplexValue {
        private Id                     _id;
        private ComplexValueForeignKey _complexValueFk;

        protected ComplexValueImpl(Id id, ComplexValueForeignKey complexValueFk) {
            _id = id;
            _complexValueFk = complexValueFk;
        }

        @Override
        public Id getId() {
            return _id;
        }

        @Override
        public void setId(Id id) {
            if (Objects.equals(_id, id)) {
                // harmless, ignore
                return;
            }
            if (_id != INVALID_ID) {
                throw new IllegalStateException("id may not be reset");
            }
            _id = id;
        }

        @Override
        public ComplexValueForeignKey getComplexValueForeignKey() {
            return _complexValueFk;
        }

        @Override
        public void setComplexValueForeignKey(ComplexValueForeignKey complexValueFk) {
            if (Objects.equals(_complexValueFk, complexValueFk)) {
                // harmless, ignore
                return;
            }
            if (_complexValueFk != INVALID_FK) {
                throw new IllegalStateException("complexValueFk may not be reset");
            }
            _complexValueFk = complexValueFk;
        }

        @Override
        public Column getColumn() {
            return _complexValueFk.getColumn();
        }

        @Override
        public int hashCode() {
            return _id.get() * 37 ^ _complexValueFk.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && Objects.equals(_id, ((ComplexValueImpl) o)._id) && _complexValueFk.equals(((ComplexValueImpl) o)._complexValueFk);
        }
    }

    /**
     * Implementation of ComplexValue.Id.
     */
    private static final class ComplexValueIdImpl extends ComplexValue.Id {
        private static final long serialVersionUID = 20130318L;

        private final int         _value;
        private final RowId       _rowId;

        protected ComplexValueIdImpl(int value, RowId rowId) {
            _value = value;
            _rowId = rowId;
        }

        @Override
        public int get() {
            return _value;
        }

        @Override
        public RowId getRowId() {
            return _rowId;
        }
    }

}
