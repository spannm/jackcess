package io.github.spannm.jackcess;

import io.github.spannm.jackcess.util.ErrorHandler;
import io.github.spannm.jackcess.util.OleBlob;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A single database table. A Table instance is retrieved from a {@link Database} instance. The Table instance provides
 * access to the table metadata as well as the table data. There are basic data operations on the Table interface (i.e.
 * {@link #iterator} {@link #addRow}, {@link #updateRow} and {@link #deleteRow}), but for advanced search and data
 * manipulation a {@link Cursor} instance should be used. New Tables can be created using a {@link TableBuilder}. The
 * {@link io.github.spannm.jackcess.util.Joiner} utility can be used to traverse table relationships (e.g. find
 * rows in another table based on a foreign-key relationship).
 * <p>
 * A Table instance is not thread-safe (see {@link Database} for more thread-safety details).
 */
public interface Table extends Iterable<Row>, TableDefinition {
    /**
     * enum which controls the ordering of the columns in a table.
     */
    enum ColumnOrder {
        /**
         * columns are ordered based on the order of the data in the table (this order does not change as columns are
         * added to the table).
         */
        DATA,
        /**
         * columns are ordered based on the "display" order (this order can be changed arbitrarily)
         */
        DISPLAY
    }

    /**
     * @return The name of the table
     */
    @Override
    String getName();

    /**
     * Whether or not this table has been marked as hidden.
     */
    @Override
    boolean isHidden();

    /**
     * Whether or not this table is a system (internal) table.
     */
    @Override
    boolean isSystem();

    @Override
    int getColumnCount();

    @Override
    Database getDatabase();

    /**
     * Gets the currently configured ErrorHandler (always non-{@code null}). This will be used to handle all errors
     * unless overridden at the Cursor level.
     */
    ErrorHandler getErrorHandler();

    /**
     * Sets a new ErrorHandler. If {@code null}, resets to using the ErrorHandler configured at the Database level.
     */
    void setErrorHandler(ErrorHandler newErrorHandler);

    /**
     * Gets the currently configured auto number insert policy.
     *
     * @see Database#isAllowAutoNumberInsert
     */
    boolean isAllowAutoNumberInsert();

    /**
     * Sets the new auto number insert policy for the Table. If {@code null}, resets to using the policy configured at
     * the Database level.
     */
    void setAllowAutoNumberInsert(Boolean allowAutoNumInsert);

    /**
     * @return All of the columns in this table (unmodifiable List)
     */
    @Override
    List<? extends Column> getColumns();

    /**
     * @return the column with the given name
     */
    @Override
    Column getColumn(String name);

    /**
     * @return the properties for this table
     */
    @Override
    PropertyMap getProperties() throws IOException;

    /**
     * @return the created date for this table if available
     */
    @Override
    LocalDateTime getCreatedDate() throws IOException;

    /**
     * Note: jackcess <i>does not automatically update the modified date of a Table</i>.
     *
     * @return the last updated date for this table if available
     */
    @Override
    LocalDateTime getUpdatedDate() throws IOException;

    /**
     * @return All of the Indexes on this table (unmodifiable List)
     */
    @Override
    List<? extends Index> getIndexes();

    /**
     * @return the index with the given name
     * @throws IllegalArgumentException if there is no index with the given name
     */
    @Override
    Index getIndex(String name);

    /**
     * @return the primary key index for this table
     * @throws IllegalArgumentException if there is no primary key index on this table
     */
    @Override
    Index getPrimaryKeyIndex();

    /**
     * @return the foreign key index joining this table to the given other table
     * @throws IllegalArgumentException if there is no relationship between this table and the given table
     */
    @Override
    Index getForeignKeyIndex(Table otherTable);

    /**
     * Converts a map of columnName -&gt; columnValue to an array of row values appropriate for a call to
     * {@link #addRow(Object...)}.
     */
    Object[] asRow(Map<String, ?> rowMap);

    /**
     * Converts a map of columnName -&gt; columnValue to an array of row values appropriate for a call to
     * {@link Cursor#updateCurrentRow(Object...)}.
     */
    Object[] asUpdateRow(Map<String, ?> rowMap);

    int getRowCount();

    /**
     * Adds a single row to this table and writes it to disk. The values are expected to be given in the order that the
     * Columns are listed by the {@link #getColumns} method. This is by default the storage order of the Columns in the
     * database, however this order can be influenced by setting the ColumnOrder via {@link Database#setColumnOrder}
     * prior to opening the Table. The {@link #asRow} method can be used to easily convert a row Map into the
     * appropriate row array for this Table.
     * <p>
     * Note, if this table has an auto-number column, the value generated will be put back into the given row array
     * (assuming the given row array is at least as long as the number of Columns in this Table).
     *
     * @param row row values for a single row. the given row array will be modified if this table contains an
     *            auto-number column, otherwise it will not be modified.
     * @return the given row values if long enough, otherwise a new array. the returned array will contain any
     *         autonumbers generated
     */
    Object[] addRow(Object... row) throws IOException;

    /**
     * Calls {@link #asRow} on the given row map and passes the result to {@link #addRow}.
     * <p>
     * Note, if this table has an auto-number column, the value generated will be put back into the given row map.
     *
     * @return the given row map, which will contain any autonumbers generated
     */
    <M extends Map<String, Object>> M addRowFromMap(M row) throws IOException;

    /**
     * Add multiple rows to this table, only writing to disk after all rows have been written, and every time a data
     * page is filled. This is much more efficient than calling {@link #addRow} multiple times.
     * <p>
     * Note, if this table has an auto-number column, the values written will be put back into the given row arrays
     * (assuming the given row array is at least as long as the number of Columns in this Table).
     * <p>
     * Most exceptions thrown from this method will be wrapped with a {@link BatchUpdateException} which gives useful
     * information in the case of a partially successful write.
     *
     * @see #addRow(Object...) for more details on row arrays
     *
     * @param rows List of Object[] row values. the rows will be modified if this table contains an auto-number column,
     *            otherwise they will not be modified.
     * @return the given row values list (unless row values were to small), with appropriately sized row values (the
     *         ones passed in if long enough). the returned arrays will contain any autonumbers generated
     */
    List<? extends Object[]> addRows(List<? extends Object[]> rows) throws IOException;

    /**
     * Calls {@link #asRow} on the given row maps and passes the results to {@link #addRows}.
     * <p>
     * Note, if this table has an auto-number column, the values generated will be put back into the appropriate row
     * maps.
     * <p>
     * Most exceptions thrown from this method will be wrapped with a {@link BatchUpdateException} which gives useful
     * information in the case of a partially successful write.
     *
     * @return the given row map list, where the row maps will contain any autonumbers generated
     */
    <M extends Map<String, Object>> List<M> addRowsFromMaps(List<M> rows) throws IOException;

    /**
     * Update the given row. Provided Row must have previously been returned from this Table.
     *
     * @return the given row, updated with the current row values
     * @throws IllegalStateException if the given row is not valid, or deleted.
     */
    Row updateRow(Row row) throws IOException;

    /**
     * Delete the given row. Provided Row must have previously been returned from this Table.
     *
     * @return the given row
     * @throws IllegalStateException if the given row is not valid
     */
    Row deleteRow(Row row) throws IOException;

    /**
     * Calls {@link #reset} on this table and returns a modifiable Iterator which will iterate through all the rows of
     * this table. Use of the Iterator follows the same restrictions as a call to {@link #getNextRow}.
     * <p>
     * For more advanced iteration, use the {@link #getDefaultCursor default cursor} directly.
     *
     * @throws UncheckedIOException if an IOException is thrown by one of the operations, the actual exception will be
     *             contained within
     */
    @Override
    Iterator<Row> iterator();

    /**
     * @return a Stream using the default Iterator.
     */
    default Stream<Row> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * After calling this method, {@link #getNextRow} will return the first row in the table, see {@link Cursor#reset}
     * (uses the {@link #getDefaultCursor default cursor}).
     */
    void reset();

    /**
     * @return The next row in this table (Column name -&gt; Column value) (uses the {@link #getDefaultCursor default
     *         cursor})
     */
    Row getNextRow() throws IOException;

    /**
     * @return a simple Cursor, initialized on demand and held by this table. This cursor backs the row traversal
     *         methods available on the Table interface. For advanced Table traversal and manipulation, use the Cursor
     *         directly.
     */
    Cursor getDefaultCursor();

    /**
     * Convenience method for constructing a new CursorBuilder for this Table.
     */
    CursorBuilder newCursor();

    /**
     * Convenience method for constructing a new OleBlob.Builder.
     */
    default OleBlob.Builder newBlob() {
        return new OleBlob.Builder();
    }
}
