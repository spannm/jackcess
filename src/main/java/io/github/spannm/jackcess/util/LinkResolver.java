/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.DatabaseImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Resolver for linked databases.
 */
@FunctionalInterface
public interface LinkResolver {
    /**
     * Legacy unrestricted link resolver.
     * <p>
     * <strong>Security warning:</strong> This resolver opens paths obtained directly from the database file, including
     * UNC/network paths and arbitrary local paths. Do not use this resolver for untrusted database files.
     */
    LinkResolver UNRESTRICTED = (linkerDb, linkeeFileName) -> {
        // if linker is read-only, open linkee read-only
        boolean readOnly = linkerDb instanceof DatabaseImpl && ((DatabaseImpl) linkerDb).isReadOnly();
        return new DatabaseBuilder()
            .withFile(new File(linkeeFileName))
            .withReadOnly(readOnly).open();
    };

    /**
     * Default link resolver used if none is provided.
     * <p>
     * Automatic linked database resolution is disabled to prevent an untrusted database from initiating a network
     * connection or opening an arbitrary local database. Applications which intentionally use trusted linked databases
     * must explicitly configure {@link #UNRESTRICTED} or a custom resolver which enforces an application-specific
     * policy.
     */
    LinkResolver DEFAULT = (linkerDb, linkeeFileName) -> {
        throw new AccessDeniedException(linkeeFileName, null,
            "Automatic linked database resolution is disabled. Configure LinkResolver.UNRESTRICTED for trusted "
                + "databases or provide a custom LinkResolver.");
    };

    /**
     * Returns the appropriate Database instance for the linkeeFileName from the given linkerDb.
     */
    Database resolveLinkedDatabase(Database linkerDb, String linkeeFileName) throws IOException;
}
