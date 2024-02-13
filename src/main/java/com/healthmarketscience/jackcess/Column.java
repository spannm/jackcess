/*
Copyright (c) 2013 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess;

import com.healthmarketscience.jackcess.complex.ComplexColumnInfo;
import com.healthmarketscience.jackcess.complex.ComplexValue;
import com.healthmarketscience.jackcess.util.ColumnValidator;

import java.io.IOException;
import java.util.Map;

/**
 * Access database column definition. A {@link Table} has a list of Column instances describing the table schema.
 * <p>
 * A Column instance is not thread-safe (see {@link Database} for more thread-safety details).
 *
 * @author James Ahlborn
 * @usage _general_class_
 */
public interface Column {
    /**
     * Meaningless placeholder object for inserting values in an autonumber column. it is not required that this value
     * be used (any passed in value is ignored), but using this placeholder may make code more obvious.
     *
     * @usage _general_field_
     */
    Object AUTO_NUMBER = "<AUTO_NUMBER>";

    /**
     * Meaningless placeholder object for updating rows which indicates that a given column should keep its existing
     * value.
     *
     * @usage _general_field_
     */
    Object KEEP_VALUE  = "<KEEP_VALUE>";

    /**
     * @usage _general_method_
     */
    Table getTable();

    /**
     * @usage _general_method_
     */
    Database getDatabase();

    /**
     * @usage _general_method_
     */
    String getName();

    /**
     * @usage _advanced_method_
     */
    boolean isVariableLength();

    /**
     * @usage _general_method_
     */
    boolean isAutoNumber();

    /**
     * @usage _advanced_method_
     */
    int getColumnIndex();

    /**
     * @usage _general_method_
     */
    DataType getType();

    /**
     * @usage _general_method_
     */
    int getSQLType() throws IOException;

    /**
     * @usage _general_method_
     */
    boolean isCompressedUnicode();

    /**
     * @usage _general_method_
     */
    byte getPrecision();

    /**
     * @usage _general_method_
     */
    byte getScale();

    /**
     * @usage _general_method_
     */
    short getLength();

    /**
     * @usage _general_method_
     */
    short getLengthInUnits();

    /**
     * Whether or not this column is "append only" (its history is tracked by a separate version history column).
     *
     * @usage _general_method_
     */
    boolean isAppendOnly();

    /**
     * Returns whether or not this is a hyperlink column (only possible for columns of type MEMO).
     *
     * @usage _general_method_
     */
    boolean isHyperlink();

    /**
     * Returns whether or not this is a calculated column. Note that jackess <b>won't interpret the calculation
     * expression</b> (but the field can be written directly).
     *
     * @usage _general_method_
     */
    boolean isCalculated();

    /**
     * Returns extended functionality for "complex" columns.
     *
     * @usage _general_method_
     */
    ComplexColumnInfo<? extends ComplexValue> getComplexInfo();

    /**
     * @return the properties for this column
     * @usage _general_method_
     */
    PropertyMap getProperties() throws IOException;

    /**
     * Returns the column which tracks the version history for an "append only" column.
     *
     * @usage _intermediate_method_
     */
    Column getVersionHistoryColumn();

    /**
     * Gets currently configured ColumnValidator (always non-{@code null}).
     *
     * @usage _intermediate_method_
     */
    ColumnValidator getColumnValidator();

    /**
     * Sets a new ColumnValidator. If {@code null}, resets to the value returned from the Database's
     * ColumnValidatorFactory (if the factory returns {@code null}, then the default is used). Autonumber columns cannot
     * have a validator instance other than the default.
     *
     * @throws IllegalArgumentException if an attempt is made to set a non-{@code null} ColumnValidator instance on an
     *             autonumber column
     * @usage _intermediate_method_
     */
    void setColumnValidator(ColumnValidator newValidator);

    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Object[] rowArray, Object value);

    @SuppressWarnings("PMD.LinguisticNaming")
    Object setRowValue(Map<String, Object> rowMap, Object value);

    Object getRowValue(Object[] rowArray);

    Object getRowValue(Map<String, ?> rowMap);
}
