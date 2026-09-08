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
import io.github.spannm.jackcess.Cursor;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.impl.CursorImpl;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builder style class for constructing a {@link Cursor} Iterable/Iterator.
 */
public class IterableBuilder implements Iterable<Row> {
    public enum Type {
        SIMPLE,
        COLUMN_MATCH,
        ROW_MATCH
    }

    private final Cursor       cursor;
    private Type               type    = Type.SIMPLE;
    private boolean            forward = true;
    private boolean            reset   = true;
    private Collection<String> columnNames;
    private ColumnMatcher      columnMatcher;
    private Object             matchPattern;

    public IterableBuilder(Cursor cursor) {
        this.cursor = cursor;
    }

    public Collection<String> getColumnNames() {
        return columnNames;
    }

    public ColumnMatcher getColumnMatcher() {
        return columnMatcher;
    }

    public boolean isForward() {
        return forward;
    }

    public boolean isReset() {
        return reset;
    }

    public Object getMatchPattern() {
        return matchPattern;
    }

    public Type getType() {
        return type;
    }

    public IterableBuilder forward() {
        return withForward(true);
    }

    public IterableBuilder reverse() {
        return withForward(false);
    }

    public IterableBuilder withForward(boolean newForward) {
        forward = newForward;
        return this;
    }

    public IterableBuilder reset(boolean newReset) {
        reset = newReset;
        return this;
    }

    public IterableBuilder withColumnNames(Collection<String> newColumnNames) {
        columnNames = newColumnNames;
        return this;
    }

    public IterableBuilder addColumnNames(Iterable<String> newColumnNames) {
        if (newColumnNames != null) {
            for (String name : newColumnNames) {
                addColumnName(name);
            }
        }
        return this;
    }

    public IterableBuilder addColumns(Iterable<? extends Column> cols) {
        if (cols != null) {
            for (Column col : cols) {
                addColumnName(col.getName());
            }
        }
        return this;
    }

    public IterableBuilder addColumnNames(String... newColumnNames) {
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

    public IterableBuilder withMatchPattern(Column columnPattern,
        Object valuePattern) {
        type = Type.COLUMN_MATCH;
        matchPattern = new AbstractMap.SimpleImmutableEntry<>(
            columnPattern, valuePattern);
        return this;
    }

    public IterableBuilder withMatchPattern(String columnNamePattern, Object valuePattern) {
        return withMatchPattern(cursor.getTable().getColumn(columnNamePattern), valuePattern);
    }

    public IterableBuilder withMatchPattern(Map<String, ?> rowPattern) {
        type = Type.ROW_MATCH;
        matchPattern = rowPattern;
        return this;
    }

    public IterableBuilder addMatchPattern(String columnNamePattern,
        Object valuePattern) {
        type = Type.ROW_MATCH;
        @SuppressWarnings("unchecked")
        Map<String, Object> newMatchPattern = (Map<String, Object>) matchPattern;
        if (newMatchPattern == null) {
            newMatchPattern = new HashMap<>();
            matchPattern = newMatchPattern;
        }
        newMatchPattern.put(columnNamePattern, valuePattern);
        return this;
    }

    public IterableBuilder withColumnMatcher(ColumnMatcher newColumnMatcher) {
        columnMatcher = newColumnMatcher;
        return this;
    }

    @Override
    public Iterator<Row> iterator() {
        return ((CursorImpl) cursor).iterator(this);
    }

    /**
     * @return a Stream using the default Iterator.
     */
    public Stream<Row> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
