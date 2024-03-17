/*
Copyright (c) 2011 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.DatabaseImpl;

import java.io.File;
import java.io.IOException;

/**
 * Resolver for linked databases.
 *
 * @author James Ahlborn
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
