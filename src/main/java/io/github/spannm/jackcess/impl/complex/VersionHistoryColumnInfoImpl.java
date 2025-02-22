package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.complex.*;
import io.github.spannm.jackcess.impl.ColumnImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Complex column info for a column which tracking the version history of an "append only" memo column.
 * <p>
 * Note, the strongly typed update/delete methods are <i>not</i> supported for version history columns (the data is
 * supposed to be immutable). That said, the "raw" update/delete methods are supported for those that <i>really</i> want
 * to muck with the version history data.
 */
public class VersionHistoryColumnInfoImpl extends ComplexColumnInfoImpl<Version> implements VersionHistoryColumnInfo {
    private final Column mvalueCol;
    private final Column mmodifiedCol;

    public VersionHistoryColumnInfoImpl(Column column, int complexId, Table typeObjTable, Table flatTable) throws IOException {
        super(column, complexId, typeObjTable, flatTable);

        Column valueCol = null;
        Column modifiedCol = null;
        for (Column col : getTypeColumns()) {
            switch (col.getType()) {
                case SHORT_DATE_TIME:
                    modifiedCol = col;
                    break;
                case MEMO:
                    valueCol = col;
                    break;
                default:
                    // ignore
            }
        }

        mvalueCol = valueCol;
        mmodifiedCol = modifiedCol;
    }

    @Override
    public void postTableLoadInit() throws IOException {
        super.postTableLoadInit();

        // link up with the actual versioned column. it should have the same name
        // as the "value" column in the type table.
        Column versionedCol = getColumn().getTable().getColumn(getValueColumn().getName());
        ((ColumnImpl) versionedCol).setVersionHistoryColumn((ColumnImpl) getColumn());
    }

    public Column getValueColumn() {
        return mvalueCol;
    }

    public Column getModifiedDateColumn() {
        return mmodifiedCol;
    }

    @Override
    public ComplexDataType getType() {
        return ComplexDataType.VERSION_HISTORY;
    }

    @Override
    public ComplexValue.Id updateValue(Version value) {
        throw new UnsupportedOperationException("This column does not support value updates");
    }

    @Override
    public void deleteValue(Version value) {
        throw new UnsupportedOperationException("This column does not support value deletes");
    }

    @Override
    public void deleteAllValues(int complexValueFk) {
        throw new UnsupportedOperationException("This column does not support value deletes");
    }

    @Override
    protected List<Version> toValues(ComplexValueForeignKey complexValueFk, List<Row> rawValues) throws IOException {
        List<Version> versions = super.toValues(complexValueFk, rawValues);

        // order versions newest to oldest
        Collections.sort(versions);

        return versions;
    }

    @Override
    protected VersionImpl toValue(ComplexValueForeignKey complexValueFk, Row rawValue) {
        ComplexValue.Id id = getValueId(rawValue);
        String value = (String) getValueColumn().getRowValue(rawValue);
        Object modifiedDate = getModifiedDateColumn().getRowValue(rawValue);

        return new VersionImpl(id, complexValueFk, value, modifiedDate);
    }

    @Override
    protected Object[] asRow(Object[] row, Version version) throws IOException {
        super.asRow(row, version);
        getValueColumn().setRowValue(row, version.getValue());
        getModifiedDateColumn().setRowValue(row, version.getModifiedDateObject());
        return row;
    }

    public static Version newVersion(String value, Object modifiedDate) {
        return newVersion(INVALID_FK, value, modifiedDate);
    }

    public static Version newVersion(ComplexValueForeignKey complexValueFk, String value, Object modifiedDate) {
        return new VersionImpl(INVALID_ID, complexValueFk, value, modifiedDate);
    }

    private static class VersionImpl extends ComplexValueImpl implements Version {
        private final String _value;
        private final Object _modifiedDate;

        private VersionImpl(Id id, ComplexValueForeignKey complexValueFk, String value, Object modifiedDate) {
            super(id, complexValueFk);
            _value = value;
            _modifiedDate = modifiedDate;
        }

        @Override
        public String getValue() {
            return _value;
        }

        @Override
        public Date getModifiedDate() {
            return (Date) _modifiedDate;
        }

        @Override
        public LocalDateTime getModifiedLocalDate() {
            return (LocalDateTime) _modifiedDate;
        }

        @Override
        public Object getModifiedDateObject() {
            return _modifiedDate;
        }

        @Override
        public int compareTo(Version o) {
            Object d1 = getModifiedDateObject();
            Object d2 = o.getModifiedDateObject();

            // sort by descending date (newest/greatest first)
            int cmp = compare(d2, d1);
            if (cmp != 0) {
                return cmp;
            }

            // use id, then complexValueFk to break ties (although we really
            // shouldn't be comparing across different columns)
            int id1 = getId().get();
            int id2 = o.getId().get();
            if (id1 != id2) {
                return id1 > id2 ? -1 : 1;
            }
            id1 = getComplexValueForeignKey().get();
            id2 = o.getComplexValueForeignKey().get();
            return Integer.compare(id2, id1);
        }

        @SuppressWarnings("unchecked")
        private static <C extends Comparable<C>> int compare(Object o1, Object o2) {
            // each date/time type (Date, LocalDateTime) is mutually Comparable, so
            // just silence the compiler
            C c1 = (C) o1;
            C c2 = (C) o2;
            return c1.compareTo(c2);
        }

        @Override
        public void update() {
            throw new UnsupportedOperationException("This column does not support value updates");
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException("This column does not support value deletes");
        }

        @Override
        public String toString() {
            return "Version(" + getComplexValueForeignKey() + "," + getId() + ") " + getModifiedDateObject() + ", " + getValue();
        }
    }

}
