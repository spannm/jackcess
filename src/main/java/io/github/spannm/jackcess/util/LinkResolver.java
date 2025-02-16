package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.DatabaseImpl;

import java.io.File;
import java.io.IOException;

/**
 * Resolver for linked databases.
 */
@FunctionalInterface
public interface LinkResolver {
    /**
     * default link resolver used if none provided
     */
    LinkResolver DEFAULT = (linkerDb, linkeeFileName) -> {
        // if linker is read-only, open linkee read-only
        boolean readOnly = linkerDb instanceof DatabaseImpl && ((DatabaseImpl) linkerDb).isReadOnly();
        return new DatabaseBuilder()
            .withFile(new File(linkeeFileName))
            .withReadOnly(readOnly).open();
    };

    /**
     * Returns the appropriate Database instance for the linkeeFileName from the given linkerDb.
     */
    Database resolveLinkedDatabase(Database linkerDb, String linkeeFileName) throws IOException;
}
