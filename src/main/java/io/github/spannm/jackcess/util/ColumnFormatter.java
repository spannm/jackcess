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
package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.expr.EvalConfig;
import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.impl.ColEvalContext;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.expr.FormatUtil;

import java.io.IOException;
import java.util.Map;

/**
 * Utility for applying Column formatting to column values for display. This utility loads the "Format" property from
 * the given column and builds an appropriate formatter (essentially leveraging the internals of the expression
 * execution engine's support for the "Format()" function). Since formats leverage the expression evaluation engine, the
 * underlying Database's {@link EvalConfig} can be used to alter how this utility formats values. Note, formatted values
 * may be suitable for <i>display only</i> (i.e. a formatted value may not be accepted as an input value to a Table
 * add/update method).
 */
public class ColumnFormatter {
    private final ColumnImpl               col;
    private final FormatEvalContext        ctx;
    private String                         fmtStr;
    private FormatUtil.StandaloneFormatter fmt;

    public ColumnFormatter(Column col) throws IOException {
        this.col = (ColumnImpl) col;
        ctx = new FormatEvalContext(this.col);
        reload();
    }

    /**
     * Returns the currently loaded "Format" property for this formatter, may be {@code null}.
     */
    public String getFormatString() {
        return fmtStr;
    }

    /**
     * Sets the given format string as the "Format" property for the underlying Column and reloads this formatter.
     *
     * @param newFmtStr the new format string. may be {@code null}, in which case the "Format" property is removed from the
     *            underlying Column
     */
    public void setFormatString(String newFmtStr) throws IOException {
        PropertyMap props = col.getProperties();
        if (!StringUtil.isEmpty(newFmtStr)) {
            props.put(PropertyMap.FORMAT_PROP, newFmtStr);
        } else {
            props.remove(PropertyMap.FORMAT_PROP);
        }
        props.save();
        reload();
    }

    /**
     * Formats the given value according to the format currently defined for the underlying Column.
     *
     * @param val a valid input value for the DataType of the underlying Column (i.e. a value which could be passed to a
     *            Table add/update method for this Column). may be {@code null}
     *
     * @return the formatted result, always non-{@code null}
     */
    public String format(Object val) {
        return ctx.format(val);
    }

    /**
     * Convenience method for retrieving the appropriate Column value from the given row array and formatting it.
     *
     * @return the formatted result, always non-{@code null}
     */
    public String getRowValue(Object[] rowArray) {
        return format(col.getRowValue(rowArray));
    }

    /**
     * Convenience method for retrieving the appropriate Column value from the given row map and formatting it.
     *
     * @return the formatted result, always non-{@code null}
     */
    public String getRowValue(Map<String, ?> rowMap) {
        return format(col.getRowValue(rowMap));
    }

    /**
     * If the properties for the underlying Column have been modified directly (or the EvalConfig for the underlying
     * Database has been modified), this method may be called to reload the format for the underlying Column.
     */
    public final void reload() throws IOException {
        fmt = null;
        fmtStr = null;

        fmtStr = (String) col.getProperties().getValue(PropertyMap.FORMAT_PROP);
        fmt = FormatUtil.createStandaloneFormatter(ctx, fmtStr, 1, 1);
    }

    /**
     * Utility class to provide an EvalContext for the expression evaluation engine format support.
     */
    private class FormatEvalContext extends ColEvalContext {
        private FormatEvalContext(ColumnImpl col) {
            super(col);
        }

        public String format(Object val) {
            try {
                return fmt.format(toValue(val)).getAsString(this);
            } catch (EvalException _ex) {
                // invalid values for a given format result in returning the value as is
                return val.toString();
            }
        }
    }
}
