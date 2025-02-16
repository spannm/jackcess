package io.github.spannm.jackcess;

import io.github.spannm.jackcess.complex.ComplexValueForeignKey;
import io.github.spannm.jackcess.util.OleBlob;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * A row of data as column name-&gt;value pairs. Values are strongly typed, and column names are case sensitive.
 */
public interface Row extends Map<String, Object> {
    /**
     * @return the id of this row
     */
    RowId getId();

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a String (DataTypes TEXT,
     * MEMO, GUID).
     */
    String getString(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Boolean (DataType
     * BOOLEAN).
     */
    Boolean getBoolean(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Byte (DataType BYTE).
     */
    Byte getByte(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Short (DataType INT).
     */
    Short getShort(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Integer (DataType LONG).
     */
    Integer getInt(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a BigDecimal (DataTypes
     * MONEY, NUMERIC).
     */
    BigDecimal getBigDecimal(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Float (DataType FLOAT).
     */
    Float getFloat(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Double (DataType
     * DOUBLE).
     */
    Double getDouble(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a Date (DataType
     * SHORT_DATE_TIME).
     *
     * @deprecated this is only valid for Database instances configured for the legacy {@link DateTimeType#DATE}. Prefer
     *             using {@link DateTimeType#LOCAL_DATE_TIME} and the corresponding {@link #getLocalDateTime} method.
     *             Using Date is being phased out and will eventually be removed.
     */
    @Deprecated
    Date getDate(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a LocalDateTime (DataType
     * SHORT_DATE_TIME or EXT_DATE_TIME). This method will only work for Database instances configured for
     * {@link DateTimeType#LOCAL_DATE_TIME}.
     */
    LocalDateTime getLocalDateTime(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a byte[] (DataTypes
     * BINARY, OLE).
     */
    byte[] getBytes(String name);

    /**
     * Convenience method which gets the value for the row with the given name, casting it to a
     * {@link ComplexValueForeignKey} (DataType COMPLEX_TYPE).
     */
    ComplexValueForeignKey getForeignKey(String name);

    /**
     * Convenience method which gets the value for the row with the given name, converting it to an {@link OleBlob}
     * (DataTypes OLE).
     * <p>
     * Note, <i>the OleBlob should be closed after use</i>.
     */
    OleBlob getBlob(String name);
}
