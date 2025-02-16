package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.complex.*;

import java.io.IOException;

/**
 * Complex column info for a column holding multiple simple values per row.
 */
public class MultiValueColumnInfoImpl extends ComplexColumnInfoImpl<SingleValue> implements MultiValueColumnInfo {
    private final Column valueCol;

    public MultiValueColumnInfoImpl(Column column, int complexId, Table typeObjTable, Table flatTable) throws IOException {
        super(column, complexId, typeObjTable, flatTable);

        valueCol = getTypeColumns().get(0);
    }

    @Override
    public ComplexDataType getType() {
        return ComplexDataType.MULTI_VALUE;
    }

    public Column getValueColumn() {
        return valueCol;
    }

    @Override
    protected SingleValueImpl toValue(ComplexValueForeignKey complexValueFk, Row rawValue) {
        ComplexValue.Id id = getValueId(rawValue);
        Object value = getValueColumn().getRowValue(rawValue);

        return new SingleValueImpl(id, complexValueFk, value);
    }

    @Override
    protected Object[] asRow(Object[] row, SingleValue value) throws IOException {
        super.asRow(row, value);
        getValueColumn().setRowValue(row, value.get());
        return row;
    }

    public static SingleValue newSingleValue(Object value) {
        return newSingleValue(INVALID_FK, value);
    }

    public static SingleValue newSingleValue(ComplexValueForeignKey complexValueFk, Object value) {
        return new SingleValueImpl(INVALID_ID, complexValueFk, value);
    }

    private static class SingleValueImpl extends ComplexValueImpl implements SingleValue {
        private Object _value;

        private SingleValueImpl(Id id, ComplexValueForeignKey complexValueFk, Object value) {
            super(id, complexValueFk);
            _value = value;
        }

        @Override
        public Object get() {
            return _value;
        }

        @Override
        public void set(Object value) {
            _value = value;
        }

        @Override
        public void update() throws IOException {
            getComplexValueForeignKey().updateMultiValue(this);
        }

        @Override
        public void delete() throws IOException {
            getComplexValueForeignKey().deleteMultiValue(this);
        }

        @Override
        public String toString() {
            return "SingleValue(" + getComplexValueForeignKey() + "," + getId() + ") " + get();
        }

    }
}
