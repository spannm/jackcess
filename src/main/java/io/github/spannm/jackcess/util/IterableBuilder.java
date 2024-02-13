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
 *
 * @author James Ahlborn
 */
public class IterableBuilder implements Iterable<Row> {
    public enum Type {
        SIMPLE,
        COLUMN_MATCH,
        ROW_MATCH;
    }

    private final Cursor       _cursor;
    private Type               _type    = Type.SIMPLE;
    private boolean            _forward = true;
    private boolean            _reset   = true;
    private Collection<String> _columnNames;
    private ColumnMatcher      _columnMatcher;
    private Object             _matchPattern;

    public IterableBuilder(Cursor cursor) {
        _cursor = cursor;
    }

    public Collection<String> getColumnNames() {
        return _columnNames;
    }

    public ColumnMatcher getColumnMatcher() {
        return _columnMatcher;
    }

    public boolean isForward() {
        return _forward;
    }

    public boolean isReset() {
        return _reset;
    }

    public Object getMatchPattern() {
        return _matchPattern;
    }

    public Type getType() {
        return _type;
    }

    public IterableBuilder forward() {
        return withForward(true);
    }

    public IterableBuilder reverse() {
        return withForward(false);
    }

    public IterableBuilder withForward(boolean forward) {
        _forward = forward;
        return this;
    }

    public IterableBuilder reset(boolean reset) {
        _reset = reset;
        return this;
    }

    public IterableBuilder withColumnNames(Collection<String> columnNames) {
        _columnNames = columnNames;
        return this;
    }

    public IterableBuilder addColumnNames(Iterable<String> columnNames) {
        if (columnNames != null) {
            for (String name : columnNames) {
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

    public IterableBuilder addColumnNames(String... columnNames) {
        if (columnNames != null) {
            for (String name : columnNames) {
                addColumnName(name);
            }
        }
        return this;
    }

    private void addColumnName(String columnName) {
        if (_columnNames == null) {
            _columnNames = new HashSet<>();
        }
        _columnNames.add(columnName);
    }

    public IterableBuilder withMatchPattern(Column columnPattern,
        Object valuePattern) {
        _type = Type.COLUMN_MATCH;
        _matchPattern = new AbstractMap.SimpleImmutableEntry<>(
            columnPattern, valuePattern);
        return this;
    }

    public IterableBuilder withMatchPattern(String columnNamePattern, Object valuePattern) {
        return withMatchPattern(_cursor.getTable().getColumn(columnNamePattern), valuePattern);
    }

    public IterableBuilder withMatchPattern(Map<String, ?> rowPattern) {
        _type = Type.ROW_MATCH;
        _matchPattern = rowPattern;
        return this;
    }

    public IterableBuilder addMatchPattern(String columnNamePattern,
        Object valuePattern) {
        _type = Type.ROW_MATCH;
        @SuppressWarnings("unchecked")
        Map<String, Object> matchPattern = (Map<String, Object>) _matchPattern;
        if (matchPattern == null) {
            matchPattern = new HashMap<>();
            _matchPattern = matchPattern;
        }
        matchPattern.put(columnNamePattern, valuePattern);
        return this;
    }

    public IterableBuilder withColumnMatcher(ColumnMatcher columnMatcher) {
        _columnMatcher = columnMatcher;
        return this;
    }

    @Override
    public Iterator<Row> iterator() {
        return ((CursorImpl) _cursor).iterator(this);
    }

    /**
     * @return a Stream using the default Iterator.
     */
    public Stream<Row> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
