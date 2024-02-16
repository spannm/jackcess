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
 * Interface which allows customization of the behavior of the {@link ImportUtil} import methods.
 *
 * @author James Ahlborn
 */
public interface ImportFilter {

    /**
     * The columns that should be used to create the imported table.
     *
     * @param destColumns the columns as determined by the import code, may be directly modified and returned
     * @param srcColumns the sql metadata, only available if importing from a JDBC source
     * @return the columns to use when creating the import table
     */
    List<ColumnBuilder> filterColumns(List<ColumnBuilder> destColumns, ResultSetMetaData srcColumns);

    /**
     * The desired values for the row.
     *
     * @param row the row data as determined by the import code, may be directly modified
     * @return the row data as it should be written to the import table. if {@code null}, the row will be skipped
     */
    Object[] filterRow(Object[] row);

}
