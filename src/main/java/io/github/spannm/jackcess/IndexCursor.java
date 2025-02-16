package io.github.spannm.jackcess;

import io.github.spannm.jackcess.util.EntryIterableBuilder;

import java.io.IOException;

/**
 * Cursor backed by an {@link Index} with extended traversal options. Table traversal will be in the order defined by
 * the backing index. Lookups which utilize the columns of the index will be fast.
 */
public interface IndexCursor extends Cursor {

    Index getIndex();

    /**
     * Finds the first row (as defined by the cursor) where the index entries match the given values. If a match is not
     * found (or an exception is thrown), the cursor is restored to its previous state.
     *
     * @param entryValues the column values for the index's columns.
     * @return the matching row or {@code null} if a match could not be found.
     */
    Row findRowByEntry(Object... entryValues) throws IOException;

    /**
     * Moves to the first row (as defined by the cursor) where the index entries match the given values. If a match is
     * not found (or an exception is thrown), the cursor is restored to its previous state.
     * <p>
     * Warning, this method <i>always</i> starts searching from the beginning of the Table (you cannot use it to find
     * successive matches).
     *
     * @param entryValues the column values for the index's columns.
     * @return {@code true} if a valid row was found with the given values, {@code false} if no row was found
     */
    boolean findFirstRowByEntry(Object... entryValues) throws IOException;

    /**
     * Moves to the first row (as defined by the cursor) where the index entries are &gt;= the given values. If a an
     * exception is thrown, the cursor is restored to its previous state.
     *
     * @param entryValues the column values for the index's columns.
     */
    void findClosestRowByEntry(Object... entryValues) throws IOException;

    /**
     * Returns {@code true} if the current row matches the given index entries.
     *
     * @param entryValues the column values for the index's columns.
     */
    boolean currentRowMatchesEntry(Object... entryValues) throws IOException;

    /**
     * Convenience method for constructing a new EntryIterableBuilder for this cursor. An EntryIterableBuilder provides
     * a variety of options for more flexible iteration based on a specific index entry.
     *
     * @param entryValues the column values for the index's columns.
     */
    EntryIterableBuilder newEntryIterable(Object... entryValues);
}
