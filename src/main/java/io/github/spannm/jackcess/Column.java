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

    /**
     * @return the table to which this column belongs
     */
    Table getTable();

    /**
     * @return the database to which this column's table belongs
     */
    Database getDatabase();

    /**
     * @return the name of this column
     */
    String getName();

    /**
     * @return {@code true} if this column's values are variable-length (e.g. TEXT, MEMO, BINARY), {@code false} if
     *         fixed-length (e.g. LONG, DOUBLE)
     */
    boolean isVariableLength();

    /**
     * @return {@code true} if this is an autonumber column, whose values are assigned automatically by the database
     */
    boolean isAutoNumber();

    /**
     * Returns the position of this column within its table, i.e. the index at which this column's value is stored in
     * the {@code Object[]} row arrays accepted/returned by {@link #getRowValue(Object[])} and
     * {@link #setRowValue(Object[], Object)}.
     */
    int getColumnIndex();

    /**
     * @return the data type of this column
     */
    DataType getType();

    /**
     * @return the {@link java.sql.Types} constant corresponding to this column's {@link DataType}
     */
    int getSQLType() throws IOException;

    /**
     * @return {@code true} if this text column uses compressed unicode storage, {@code false} otherwise (not
     *         applicable to non-text columns)
     */
    boolean isCompressedUnicode();

    /**
     * @return the numeric precision of this column (only meaningful for DataTypes NUMERIC and MONEY)
     */
    byte getPrecision();

    /**
     * @return the numeric scale of this column (only meaningful for DataTypes NUMERIC and MONEY)
     */
    byte getScale();

    /**
     * @return the length of this column in bytes, as stored in the database
     */
    short getLength();

    /**
     * @return the length of this column in "units" appropriate for its type (e.g. characters for TEXT columns),
     *         which may differ from {@link #getLength()} for columns whose storage unit is wider than one byte
     */
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

    /**
     * Sets this column's value at the position given by {@link #getColumnIndex()} within {@code rowArray}.
     *
     * @param rowArray a row value array, indexed by {@link #getColumnIndex()}
     * @param value the new value for this column
     * @return the previous value at that position
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Object[] rowArray, Object value);

    /**
     * Sets this column's value, keyed by {@link #getName()}, within {@code rowMap}.
     *
     * @param rowMap a row value map, keyed by column name
     * @param value the new value for this column
     * @return the previous value for this column's name
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Map<String, Object> rowMap, Object value);

    /**
     * @param rowArray a row value array, indexed by {@link #getColumnIndex()}
     * @return this column's value at the position given by {@link #getColumnIndex()} within {@code rowArray}
     */
    Object getRowValue(Object[] rowArray);

    /**
     * @param rowMap a row value map, keyed by column name
     * @return this column's value, keyed by {@link #getName()}, within {@code rowMap}
     */
    Object getRowValue(Map<String, ?> rowMap);
}
