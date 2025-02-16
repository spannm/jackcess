package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.complex.ComplexValueForeignKey;
import io.github.spannm.jackcess.util.OleBlob;
import io.github.spannm.jackcess.util.ToStringBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * A row of data as column-&gt;value pairs.
 * <p>
 * Note that the {@link #equals} and {@link #hashCode} methods work on the row contents <i>only</i> (i.e. they ignore
 * the id).
 */
public class RowImpl extends LinkedHashMap<String, Object> implements Row {
    private static final long serialVersionUID = 20130314L;

    private final RowIdImpl   _id;

    public RowImpl(RowIdImpl id) {
        _id = id;
    }

    public RowImpl(RowIdImpl id, int expectedSize) {
        super(expectedSize);
        _id = id;
    }

    public RowImpl(Row row) {
        super(row);
        _id = (RowIdImpl) row.getId();
    }

    @Override
    public RowIdImpl getId() {
        return _id;
    }

    @Override
    public String getString(String name) {
        return (String) get(name);
    }

    @Override
    public Boolean getBoolean(String name) {
        return (Boolean) get(name);
    }

    @Override
    public Byte getByte(String name) {
        return (Byte) get(name);
    }

    @Override
    public Short getShort(String name) {
        return (Short) get(name);
    }

    @Override
    public Integer getInt(String name) {
        return (Integer) get(name);
    }

    @Override
    public BigDecimal getBigDecimal(String name) {
        return (BigDecimal) get(name);
    }

    @Override
    public Float getFloat(String name) {
        return (Float) get(name);
    }

    @Override
    public Double getDouble(String name) {
        return (Double) get(name);
    }

    @Override
    public Date getDate(String name) {
        return (Date) get(name);
    }

    @Override
    public LocalDateTime getLocalDateTime(String name) {
        return (LocalDateTime) get(name);
    }

    @Override
    public byte[] getBytes(String name) {
        return (byte[]) get(name);
    }

    @Override
    public ComplexValueForeignKey getForeignKey(String name) {
        return (ComplexValueForeignKey) get(name);
    }

    @Override
    public OleBlob getBlob(String name) {
        byte[] bytes = getBytes(name);
        return bytes != null ? OleBlob.Builder.fromInternalData(bytes) : null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.valueBuilder("Row[" + _id + "]")
            .append(null, this)
            .toString();
    }
}
