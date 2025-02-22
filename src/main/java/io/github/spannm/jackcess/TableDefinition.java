package io.github.spannm.jackcess;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The definition of a single database table. A TableDefinition instance is retrieved from a {@link TableMetaData}
 * instance. The TableDefinition instance only provides access to the table metadata, but no table data.
 * <p>
 * A TableDefinition instance is not thread-safe (see {@link Database} for more thread-safety details).
 */
public interface TableDefinition {
    /**
     * @return The name of the table
     */
    String getName();

    /**
     * Whether or not this table has been marked as hidden.
     */
    boolean isHidden();

    /**
     * Whether or not this table is a system (internal) table.
     */
    boolean isSystem();

    int getColumnCount();

    Database getDatabase();

    /**
     * @return All of the columns in this table (unmodifiable List)
     */
    List<? extends Column> getColumns();

    /**
     * @return the column with the given name
     */
    Column getColumn(String name);

    /**
     * @return the properties for this table
     */
    PropertyMap getProperties() throws IOException;

    /**
     * @return the created date for this table if available
     */
    LocalDateTime getCreatedDate() throws IOException;

    /**
     * Note: jackcess <i>does not automatically update the modified date of a Table</i>.
     *
     * @return the last updated date for this table if available
     */
    LocalDateTime getUpdatedDate() throws IOException;

    /**
     * @return All of the Indexes on this table (unmodifiable List)
     */
    List<? extends Index> getIndexes();

    /**
     * @return the index with the given name
     * @throws IllegalArgumentException if there is no index with the given name
     */
    Index getIndex(String name);

    /**
     * @return the primary key index for this table
     * @throws IllegalArgumentException if there is no primary key index on this table
     */
    Index getPrimaryKeyIndex();

    /**
     * @return the foreign key index joining this table to the given other table
     * @throws IllegalArgumentException if there is no relationship between this table and the given table
     */
    Index getForeignKeyIndex(Table otherTable);
}
