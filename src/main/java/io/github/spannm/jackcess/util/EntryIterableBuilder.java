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
import io.github.spannm.jackcess.IndexCursor;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.impl.IndexCursorImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builder style class for constructing an {@link IndexCursor} entry Iterable/Iterator.
 */
public class EntryIterableBuilder implements Iterable<Row> {
    private final IndexCursor  cursor;

    private Collection<String> columnNames;
    private Object[]           entryValues;
    private ColumnMatcher      columnMatcher;

    public EntryIterableBuilder(IndexCursor cursor, Object... entryValues) {
        this.cursor = cursor;
        this.entryValues = entryValues;
    }

    public Collection<String> getColumnNames() {
        return columnNames;
    }

    public ColumnMatcher getColumnMatcher() {
        return columnMatcher;
    }

    public Object[] getEntryValues() {
        return entryValues;
    }

    public EntryIterableBuilder withColumnNames(Collection<String> newColumnNames) {
        columnNames = newColumnNames;
        return this;
    }

    public EntryIterableBuilder addColumnNames(Iterable<String> newColumnNames) {
        if (newColumnNames != null) {
            for (String name : newColumnNames) {
                addColumnName(name);
            }
        }
        return this;
    }

    public EntryIterableBuilder addColumns(Iterable<? extends Column> cols) {
        if (cols != null) {
            for (Column col : cols) {
                addColumnName(col.getName());
            }
        }
        return this;
    }

    public EntryIterableBuilder addColumnNames(String... newColumnNames) {
        if (newColumnNames != null) {
            for (String name : newColumnNames) {
                addColumnName(name);
            }
        }
        return this;
    }

    private void addColumnName(String columnName) {
        if (columnNames == null) {
            columnNames = new HashSet<>();
        }
        columnNames.add(columnName);
    }

    public EntryIterableBuilder withEntryValues(Object... newEntryValues) {
        entryValues = newEntryValues;
        return this;
    }

    public EntryIterableBuilder withColumnMatcher(ColumnMatcher newColumnMatcher) {
        columnMatcher = newColumnMatcher;
        return this;
    }

    @Override
    public Iterator<Row> iterator() {
        return ((IndexCursorImpl) cursor).entryIterator(this);
    }

    /**
     * @return a Stream using the default Iterator.
     */
    public Stream<Row> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
