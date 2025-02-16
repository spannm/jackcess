package io.github.spannm.jackcess;

import java.io.IOException;
import java.util.List;

/**
 * Access database index definition. A {@link Table} has a list of Index instances. Indexes can enable fast searches and
 * ordered traversal on a Table (for the indexed columns). These features can be utilized via an {@link IndexCursor}.
 */
public interface Index {

    Table getTable();

    String getName();

    boolean isPrimaryKey();

    boolean isForeignKey();

    int getColumnCount();

    /**
     * @return the Columns for this index (unmodifiable)
     */
    List<? extends Index.Column> getColumns();

    /**
     * @return the Index referenced by this Index's ForeignKeyReference (if it has one), otherwise {@code null}.
     */
    Index getReferencedIndex() throws IOException;

    /**
     * Whether or not {@code null} values are actually recorded in the index.
     */
    boolean shouldIgnoreNulls();

    /**
     * Whether or not index entries must be unique.
     * <p>
     * Some notes about uniqueness:
     * <ul>
     * <li>Access does not seem to consider multiple {@code null} entries invalid for a unique index</li>
     * <li>text indexes collapse case, and Access seems to compare <b>only</b> the index entry bytes, therefore two
     * strings which differ only in case <i>will violate</i> the unique constraint</li>
     * </ul>
     */
    boolean isUnique();

    /**
     * Whether or not values are required for index columns.
     */
    boolean isRequired();

    /**
     * Convenience method for constructing a new CursorBuilder for this Index.
     */
    CursorBuilder newCursor();

    /**
     * Information about a Column in an Index
     */
    interface Column {

        io.github.spannm.jackcess.Column getColumn();

        boolean isAscending();

        int getColumnIndex();

        String getName();
    }
}
