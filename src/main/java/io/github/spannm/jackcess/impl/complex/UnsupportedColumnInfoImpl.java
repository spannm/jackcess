package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.complex.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complex column info for an unsupported complex type.
 *
 * @author James Ahlborn
 */
public class UnsupportedColumnInfoImpl
    extends ComplexColumnInfoImpl<UnsupportedValue>
    implements UnsupportedColumnInfo {

    public UnsupportedColumnInfoImpl(Column column, int complexId,
        Table typeObjTable, Table flatTable) throws IOException {
        super(column, complexId, typeObjTable, flatTable);
    }

    public List<Column> getValueColumns() {
        return getTypeColumns();
    }

    @Override
    public ComplexDataType getType() {
        return ComplexDataType.UNSUPPORTED;
    }

    @Override
    protected UnsupportedValueImpl toValue(
        ComplexValueForeignKey complexValueFk,
        Row rawValue) {
        ComplexValue.Id id = getValueId(rawValue);

        Map<String, Object> values = new LinkedHashMap<>();
        for (Column col : getValueColumns()) {
            col.setRowValue(values, col.getRowValue(rawValue));
        }

        return new UnsupportedValueImpl(id, complexValueFk, values);
    }

    @Override
    protected Object[] asRow(Object[] row, UnsupportedValue value) throws IOException {
        super.asRow(row, value);

        Map<String, Object> values = value.getValues();
        for (Column col : getValueColumns()) {
            col.setRowValue(row, col.getRowValue(values));
        }

        return row;
    }

    public static UnsupportedValue newValue(Map<String, ?> values) {
        return newValue(INVALID_FK, values);
    }

    public static UnsupportedValue newValue(
        ComplexValueForeignKey complexValueFk, Map<String, ?> values) {
        return new UnsupportedValueImpl(INVALID_ID, complexValueFk,
            new LinkedHashMap<>(values));
    }

    private static class UnsupportedValueImpl extends ComplexValueImpl implements UnsupportedValue {
        private final Map<String, Object> _values;

        private UnsupportedValueImpl(Id id, ComplexValueForeignKey complexValueFk,
            Map<String, Object> values) {
            super(id, complexValueFk);
            _values = values;
        }

        @Override
        public Map<String, Object> getValues() {
            return _values;
        }

        @Override
        public Object get(String columnName) {
            return getValues().get(columnName);
        }

        @Override
        public void set(String columnName, Object value) {
            getValues().put(columnName, value);
        }

        @Override
        public void update() throws IOException {
            getComplexValueForeignKey().updateUnsupportedValue(this);
        }

        @Override
        public void delete() throws IOException {
            getComplexValueForeignKey().deleteUnsupportedValue(this);
        }

        @Override
        public String toString() {
            return "UnsupportedValue(" + getComplexValueForeignKey() + "," + getId() + ") " + getValues();
        }
    }
}
