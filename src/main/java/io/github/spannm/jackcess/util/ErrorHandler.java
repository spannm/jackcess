/*
Copyright (c) 2008 Health Market Science, Inc.

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
import io.github.spannm.jackcess.Table;

import java.io.IOException;

/**
 * Handler for errors encountered while reading a column of row data from a Table. An instance of this class may be
 * configured at the Database, Table, or Cursor level to customize error handling as desired. The default instance used
 * is {@link #DEFAULT}, which just rethrows any exceptions encountered.
 */
@FunctionalInterface
public interface ErrorHandler {
    /**
     * default error handler used if none provided (just rethrows exception)
     */
    ErrorHandler DEFAULT = (column, columnData, location, error) -> {
        // really can only be RuntimeException or IOException
        if (error instanceof IOException) {
            throw (IOException) error;
        }
        throw (RuntimeException) error;
    };

    /**
     * Handles an error encountered while reading a column of data from a Table row. Handler may either throw an
     * exception (which will be propagated back to the caller) or return a replacement for this row's column value (in
     * which case the row will continue to be read normally).
     *
     * @param column the info for the column being read
     * @param columnData the actual column data for the column being read (which may be {@code null} depending on when
     *            the exception was thrown during the reading process)
     * @param location the current location of the error
     * @param error the error that was encountered
     *
     * @return replacement for this row's column
     */
    Object handleRowError(Column column,
        byte[] columnData,
        Location location,
        Exception error) throws IOException;

    /**
     * Provides location information for an error.
     */
    interface Location {
        /**
         * @return the table in which the error occurred
         */
        Table getTable();

        /**
         * Contains details about the errored row, useful for debugging.
         */
        @Override
        String toString();
    }
}
