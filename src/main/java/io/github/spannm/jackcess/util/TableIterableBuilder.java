package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.impl.DatabaseImpl;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builder style class for constructing a {@link Database} Iterable/Iterator for {@link Table}s. By default, normal
 * (non-system, non-linked tables) and linked tables are included and system tables are not.
 *
 * @author James Ahlborn
 */
public class TableIterableBuilder implements Iterable<Table> {
    private final Database _db;
    private boolean        _includeNormalTables = true;
    private boolean        _includeSystemTables;
    private boolean        _includeLinkedTables = true;

    public TableIterableBuilder(Database db) {
        _db = db;
    }

    public boolean isIncludeNormalTables() {
        return _includeNormalTables;
    }

    public boolean isIncludeSystemTables() {
        return _includeSystemTables;
    }

    public boolean isIncludeLinkedTables() {
        return _includeLinkedTables;
    }

    public TableIterableBuilder withIncludeNormalTables(boolean includeNormalTables) {
        _includeNormalTables = includeNormalTables;
        return this;
    }

    public TableIterableBuilder withIncludeSystemTables(boolean includeSystemTables) {
        _includeSystemTables = includeSystemTables;
        return this;
    }

    public TableIterableBuilder withIncludeLinkedTables(boolean includeLinkedTables) {
        _includeLinkedTables = includeLinkedTables;
        return this;
    }

    /**
     * Convenience method to set the flags to include only non-linked (local) user tables.
     */
    public TableIterableBuilder withLocalUserTablesOnly() {
        withIncludeNormalTables(true);
        withIncludeSystemTables(false);
        return withIncludeLinkedTables(false);
    }

    /**
     * Convenience method to set the flags to include only system tables.
     */
    public TableIterableBuilder withSystemTablesOnly() {
        withIncludeNormalTables(false);
        withIncludeSystemTables(true);
        return withIncludeLinkedTables(false);
    }

    @Override
    public Iterator<Table> iterator() {
        return ((DatabaseImpl) _db).iterator(this);
    }

    /**
     * @return a Stream using the default Iterator.
     */
    public Stream<Table> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
