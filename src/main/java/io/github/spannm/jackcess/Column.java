package io.github.spannm.jackcess;

import io.github.spannm.jackcess.complex.ComplexColumnInfo;
import io.github.spannm.jackcess.complex.ComplexValue;
import io.github.spannm.jackcess.util.ColumnValidator;

import java.io.IOException;
import java.util.Map;

/**
 * Access database column definition. A {@link Table} has a list of Column instances describing the table schema.
 * <p>
 * A Column instance is not thread-safe (see {@link Database} for more thread-safety details).
 */
public interface Column {
    /**
     * Meaningless placeholder object for inserting values in an autonumber column. it is not required that this value
     * be used (any passed in value is ignored), but using this placeholder may make code more obvious.
     */
    Object AUTO_NUMBER = "<AUTO_NUMBER>";

    /**
     * Meaningless placeholder object for updating rows which indicates that a given column should keep its existing
     * value.
     */
    Object KEEP_VALUE  = "<KEEP_VALUE>";

    Table getTable();

    Database getDatabase();

    String getName();

    boolean isVariableLength();

    boolean isAutoNumber();

    int getColumnIndex();

    DataType getType();

    int getSQLType() throws IOException;

    boolean isCompressedUnicode();

    byte getPrecision();

    byte getScale();

    short getLength();

    short getLengthInUnits();

    /**
     * Whether or not this column is "append only" (its history is tracked by a separate version history column).
     */
    boolean isAppendOnly();

    /**
     * Returns whether or not this is a hyperlink column (only possible for columns of type MEMO).
     */
    boolean isHyperlink();

    /**
     * Returns whether or not this is a calculated column. Note that jackess <b>won't interpret the calculation
     * expression</b> (but the field can be written directly).
     */
    boolean isCalculated();

    /**
     * Returns extended functionality for "complex" columns.
     */
    ComplexColumnInfo<? extends ComplexValue> getComplexInfo();

    /**
     * @return the properties for this column
     */
    PropertyMap getProperties() throws IOException;

    /**
     * Returns the column which tracks the version history for an "append only" column.
     */
    Column getVersionHistoryColumn();

    /**
     * Gets currently configured ColumnValidator (always non-{@code null}).
     */
    ColumnValidator getColumnValidator();

    /**
     * Sets a new ColumnValidator. If {@code null}, resets to the value returned from the Database's
     * ColumnValidatorFactory (if the factory returns {@code null}, then the default is used). Autonumber columns cannot
     * have a validator instance other than the default.
     *
     * @throws IllegalArgumentException if an attempt is made to set a non-{@code null} ColumnValidator instance on an
     *             autonumber column
     */
    void setColumnValidator(ColumnValidator newValidator);

    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Object[] rowArray, Object value);

    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Map<String, Object> rowMap, Object value);

    Object getRowValue(Object[] rowArray);

    Object getRowValue(Map<String, ?> rowMap);
}
