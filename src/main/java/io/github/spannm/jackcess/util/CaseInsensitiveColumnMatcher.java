/*
Copyright (c) 2010 James Ahlborn

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

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.impl.ColumnImpl;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Concrete implementation of ColumnMatcher which tests textual columns case-insensitively ({@link DataType#TEXT} and
 * {@link DataType#MEMO}), and all other columns using simple equality.
 */
public class CaseInsensitiveColumnMatcher implements ColumnMatcher {

    public static final CaseInsensitiveColumnMatcher INSTANCE =
        new CaseInsensitiveColumnMatcher();

    @Override
    public boolean matches(Table table, String columnName, Object value1,
        Object value2) {
        if (!table.getColumn(columnName).getType().isTextual()) {
            // use simple equality
            return SimpleColumnMatcher.INSTANCE.matches(table, columnName,
                value1, value2);
        }

        // convert both values to Strings and compare case-insensitively
        try {
            CharSequence cs1 = ColumnImpl.toCharSequence(value1);
            CharSequence cs2 = ColumnImpl.toCharSequence(value2);

            return cs1 == cs2 || cs1 != null && cs2 != null && cs1.toString().equalsIgnoreCase(cs2.toString());
        } catch (IOException _ex) {
            throw new UncheckedIOException("Could not read column " + columnName
                + " value", _ex);
        }
    }

}
