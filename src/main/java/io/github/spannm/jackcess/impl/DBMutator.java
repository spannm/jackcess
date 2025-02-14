package io.github.spannm.jackcess.impl;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Common helper class used to maintain state during database mutation.
 *
 * @author James Ahlborn
 */
abstract class DBMutator {
    private final DatabaseImpl _database;

    protected DBMutator(DatabaseImpl database) {
        _database = database;
    }

    public DatabaseImpl getDatabase() {
        return _database;
    }

    public JetFormat getFormat() {
        return _database.getFormat();
    }

    public PageChannel getPageChannel() {
        return _database.getPageChannel();
    }

    public Charset getCharset() {
        return _database.getCharset();
    }

    public int reservePageNumber() throws IOException {
        return getPageChannel().allocateNewPage();
    }

    public static int calculateNameLength(String name) {
        return name.length() * JetFormat.TEXT_FIELD_UNIT_SIZE + 2;
    }

    protected ColumnImpl.SortOrder getDbSortOrder() {
        try {
            return _database.getDefaultSortOrder();
        } catch (IOException e) {
            // ignored, just use the jet format default
        }
        return null;
    }
}
