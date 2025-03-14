package io.github.spannm.jackcess;

import io.github.spannm.jackcess.util.ColumnMatcher;
import io.github.spannm.jackcess.util.ErrorHandler;
import io.github.spannm.jackcess.util.IterableBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Manages iteration for a {@link Table}. Different cursors provide different methods of traversing a table. Cursors
 * should be fairly robust in the face of table modification during traversal (although depending on how the table is
 * traversed, row updates may or may not be seen). Multiple cursors may traverse the same table simultaneously.
 * <p>
 * Basic cursors will generally iterate table data in the order it appears in the database and searches will require
 * scanning the entire table. Additional features are available when utilizing an {@link Index} backed
 * {@link IndexCursor}.
 * <p>
 * The {@link CursorBuilder} provides a variety of static utility methods to construct cursors with given
 * characteristics or easily search for specific values as well as friendly and flexible construction options.
 * <p>
 * A Cursor instance is not thread-safe (see {@link Database} for more thread-safety details).
 */
public interface Cursor extends Iterable<Row> {

    Id getId();

    Table getTable();

    /**
     * Gets the currently configured ErrorHandler (always non-{@code null}). This will be used to handle all errors.
     */
    ErrorHandler getErrorHandler();

    /**
     * Sets a new ErrorHandler. If {@code null}, resets to using the ErrorHandler configured at the Table level.
     */
    void setErrorHandler(ErrorHandler newErrorHandler);

    /**
     * Returns the currently configured ColumnMatcher, always non-{@code null}.
     */
    ColumnMatcher getColumnMatcher();

    /**
     * Sets a new ColumnMatcher. If {@code null}, resets to using the default matcher (default depends on Cursor type).
     */
    void setColumnMatcher(ColumnMatcher columnMatcher);

    /**
     * Returns the current state of the cursor which can be restored at a future point in time by a call to
     * {@link #restoreSavepoint}.
     * <p>
     * Savepoints may be used across different cursor instances for the same table, but they must have the same
     * {@link Id}.
     */
    Savepoint getSavepoint();

    /**
     * Moves the cursor to a savepoint previously returned from {@link #getSavepoint}.
     *
     * @throws IllegalArgumentException if the given savepoint does not have a cursorId equal to this cursor's id
     */
    void restoreSavepoint(Savepoint savepoint) throws IOException;

    /**
     * Resets this cursor for forward traversal. Calls {@link #beforeFirst}.
     */
    void reset();

    /**
     * Resets this cursor for forward traversal (sets cursor to before the first row).
     */
    void beforeFirst();

    /**
     * Resets this cursor for reverse traversal (sets cursor to after the last row).
     */
    void afterLast();

    /**
     * Returns {@code true} if the cursor is currently positioned before the first row, {@code false} otherwise.
     */
    boolean isBeforeFirst() throws IOException;

    /**
     * Returns {@code true} if the cursor is currently positioned after the last row, {@code false} otherwise.
     */
    boolean isAfterLast() throws IOException;

    /**
     * Returns {@code true} if the row at which the cursor is currently positioned is deleted, {@code false} otherwise
     * (including invalid rows).
     */
    boolean isCurrentRowDeleted() throws IOException;

    /**
     * Calls {@link #beforeFirst} on this cursor and returns a modifiable Iterator which will iterate through all the
     * rows of this table. Use of the Iterator follows the same restrictions as a call to {@link #getNextRow}.
     * <p>
     * For more flexible iteration see {@link #newIterable}.
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
     * Convenience method for constructing a new IterableBuilder for this cursor. An IterableBuilder provides a variety
     * of options for more flexible iteration.
     */
    IterableBuilder newIterable();

    /**
     * Delete the current row.
     * <p>
     * Note, re-deleting an already deleted row is allowed (it does nothing).
     *
     * @throws IllegalStateException if the current row is not valid (at beginning or end of table)
     */
    void deleteCurrentRow() throws IOException;

    /**
     * Update the current row.
     *
     * @return the given row values if long enough, otherwise a new array, updated with the current row values
     * @throws IllegalStateException if the current row is not valid (at beginning or end of table), or deleted.
     */
    Object[] updateCurrentRow(Object... row) throws IOException;

    /**
     * Update the current row.
     *
     * @return the given row, updated with the current row values
     * @throws IllegalStateException if the current row is not valid (at beginning or end of table), or deleted.
     */
    <M extends Map<String, Object>> M updateCurrentRowFromMap(M row) throws IOException;

    /**
     * Moves to the next row in the table and returns it.
     *
     * @return The next row in this table (Column name -&gt; Column value), or {@code null} if no next row is found
     */
    Row getNextRow() throws IOException;

    /**
     * Moves to the next row in the table and returns it.
     *
     * @param columnNames Only column names in this collection will be returned
     * @return The next row in this table (Column name -&gt; Column value), or {@code null} if no next row is found
     */
    Row getNextRow(Collection<String> columnNames) throws IOException;

    /**
     * Moves to the previous row in the table and returns it.
     *
     * @return The previous row in this table (Column name -&gt; Column value), or {@code null} if no previous row is
     *         found
     */
    Row getPreviousRow() throws IOException;

    /**
     * Moves to the previous row in the table and returns it.
     *
     * @param columnNames Only column names in this collection will be returned
     * @return The previous row in this table (Column name -&gt; Column value), or {@code null} if no previous row is
     *         found
     */
    Row getPreviousRow(Collection<String> columnNames) throws IOException;

    /**
     * Moves to the next row as defined by this cursor.
     *
     * @return {@code true} if a valid next row was found, {@code false} otherwise
     */
    boolean moveToNextRow() throws IOException;

    /**
     * Moves to the previous row as defined by this cursor.
     *
     * @return {@code true} if a valid previous row was found, {@code false} otherwise
     */
    boolean moveToPreviousRow() throws IOException;

    /**
     * Moves to the row with the given rowId. If the row is not found (or an exception is thrown), the cursor is
     * restored to its previous state.
     *
     * @return {@code true} if a valid row was found with the given id, {@code false} if no row was found
     */
    boolean findRow(RowId rowId) throws IOException;

    /**
     * Moves to the first row (as defined by the cursor) where the given column has the given value. This may be more
     * efficient on some cursors than others. If a match is not found (or an exception is thrown), the cursor is
     * restored to its previous state.
     * <p>
     * Warning, this method <i>always</i> starts searching from the beginning of the Table (you cannot use it to find
     * successive matches).
     *
     * @param columnPattern column from the table for this cursor which is being matched by the valuePattern
     * @param valuePattern value which is equal to the corresponding value in the matched row. If this object is an
     *            instance of {@link java.util.function.Predicate}, it will be applied to the potential row value
     *            instead (overriding any configured ColumnMatcher)
     * @return {@code true} if a valid row was found with the given value, {@code false} if no row was found
     */
    boolean findFirstRow(Column columnPattern, Object valuePattern) throws IOException;

    /**
     * Moves to the next row (as defined by the cursor) where the given column has the given value. This may be more
     * efficient on some cursors than others. If a match is not found (or an exception is thrown), the cursor is
     * restored to its previous state.
     *
     * @param columnPattern column from the table for this cursor which is being matched by the valuePattern
     * @param valuePattern value which is equal to the corresponding value in the matched row. If this object is an
     *            instance of {@link java.util.function.Predicate}, it will be applied to the potential row value
     *            instead (overriding any configured ColumnMatcher)
     * @return {@code true} if a valid row was found with the given value, {@code false} if no row was found
     */
    boolean findNextRow(Column columnPattern, Object valuePattern) throws IOException;

    /**
     * Moves to the first row (as defined by the cursor) where the given columns have the given values. This may be more
     * efficient on some cursors than others. If a match is not found (or an exception is thrown), the cursor is
     * restored to its previous state.
     * <p>
     * Warning, this method <i>always</i> starts searching from the beginning of the Table (you cannot use it to find
     * successive matches).
     *
     * @param rowPattern column names and values which must be equal to the corresponding values in the matched row. If
     *            a value is an instance of {@link java.util.function.Predicate}, it will be applied to the potential
     *            row value instead (overriding any configured ColumnMatcher)
     * @return {@code true} if a valid row was found with the given values, {@code false} if no row was found
     */
    boolean findFirstRow(Map<String, ?> rowPattern) throws IOException;

    /**
     * Moves to the next row (as defined by the cursor) where the given columns have the given values. This may be more
     * efficient on some cursors than others. If a match is not found (or an exception is thrown), the cursor is
     * restored to its previous state.
     *
     * @param rowPattern column names and values which must be equal to the corresponding values in the matched row. If
     *            a value is an instance of {@link java.util.function.Predicate}, it will be applied to the potential
     *            row value instead (overriding any configured ColumnMatcher)
     * @return {@code true} if a valid row was found with the given values, {@code false} if no row was found
     */
    boolean findNextRow(Map<String, ?> rowPattern) throws IOException;

    /**
     * Returns {@code true} if the current row matches the given pattern.
     *
     * @param columnPattern column from the table for this cursor which is being matched by the valuePattern
     * @param valuePattern value which is equal to the corresponding value in the matched row. If this object is an
     *            instance of {@link java.util.function.Predicate}, it will be applied to the potential row value
     *            instead (overriding any configured ColumnMatcher)
     */
    boolean currentRowMatches(Column columnPattern, Object valuePattern) throws IOException;

    /**
     * Returns {@code true} if the current row matches the given pattern.
     *
     * @param rowPattern column names and values which must be equal to the corresponding values in the matched row. If
     *            a value is an instance of {@link java.util.function.Predicate}, it will be applied to the potential
     *            row value instead (overriding any configured ColumnMatcher)
     */
    boolean currentRowMatches(Map<String, ?> rowPattern) throws IOException;

    /**
     * Moves forward as many rows as possible up to the given number of rows.
     *
     * @return the number of rows moved.
     */
    int moveNextRows(int numRows) throws IOException;

    /**
     * Moves backward as many rows as possible up to the given number of rows.
     *
     * @return the number of rows moved.
     */
    int movePreviousRows(int numRows) throws IOException;

    /**
     * Returns the current row in this cursor (Column name -&gt; Column value).
     */
    Row getCurrentRow() throws IOException;

    /**
     * Returns the current row in this cursor (Column name -&gt; Column value).
     *
     * @param columnNames Only column names in this collection will be returned
     */
    Row getCurrentRow(Collection<String> columnNames) throws IOException;

    /**
     * Returns the given column from the current row.
     */
    Object getCurrentRowValue(Column column) throws IOException;

    /**
     * Updates a single value in the current row.
     *
     * @throws IllegalStateException if the current row is not valid (at beginning or end of table), or deleted.
     */
    void setCurrentRowValue(Column column, Object value) throws IOException;

    /**
     * Identifier for a cursor. Will be equal to any other cursor of the same type for the same table. Primarily used to
     * check the validity of a Savepoint.
     */
    interface Id {
    }

    /**
     * Value object which maintains the current position of the cursor.
     */
    interface Position {
        /**
         * Returns the unique RowId of the position of the cursor.
         */
        RowId getRowId();
    }

    /**
     * Value object which represents a complete save state of the cursor. Savepoints are created by calling
     * {@link Cursor#getSavepoint} and used by calling {@link Cursor#restoreSavepoint} to return the the cursor state at
     * the time the Savepoint was created.
     */
    interface Savepoint {
        Id getCursorId();

        Position getCurrentPosition();
    }

}
