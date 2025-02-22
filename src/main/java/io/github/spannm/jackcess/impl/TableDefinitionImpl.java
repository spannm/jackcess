package io.github.spannm.jackcess.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A database table definition which does not allow any actual data interaction (read or write).
 * <p>
 * Note, in an ideal world, TableImpl would extend TableDefinitionImpl. However, since TableDefinitionImpl came later,
 * it was easier to do it this way and avoid a lot of unnecessary code shuffling.
 * <p>
 * Is not thread-safe.
 */
public class TableDefinitionImpl extends TableImpl {
    protected TableDefinitionImpl(DatabaseImpl database, ByteBuffer tableBuffer, int pageNumber, String name, int flags) throws IOException {
        super(database, tableBuffer, pageNumber, name, flags);
    }

    @Override
    protected List<? extends Object[]> addRows(List<? extends Object[]> rows, final boolean isBatchWrite) {
        // all row additions eventually flow through this method
        throw new UnsupportedOperationException(withErrorContext("TableDefinition has no data access"));
    }

    @Override
    public RowState createRowState() {
        // RowState is needed for all traversal operations, so this kills any data
        // reading as well as update/delete methods
        throw new UnsupportedOperationException(withErrorContext("TableDefinition has no data access"));
    }
}
