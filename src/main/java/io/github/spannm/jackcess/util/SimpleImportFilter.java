/*
Copyright (c) 2007 Health Market Science, Inc.

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

import io.github.spannm.jackcess.ColumnBuilder;

import java.sql.ResultSetMetaData;
import java.util.List;

/**
 * Simple concrete implementation of ImportFilter which just returns the given values.
 */
public class SimpleImportFilter implements ImportFilter {

    public static final SimpleImportFilter INSTANCE = new SimpleImportFilter();

    public SimpleImportFilter() {
    }

    @Override
    public List<ColumnBuilder> filterColumns(List<ColumnBuilder> destColumns, ResultSetMetaData srcColumns) {
        return destColumns;
    }

    @Override
    public Object[] filterRow(Object[] row) {
        return row;
    }

}
