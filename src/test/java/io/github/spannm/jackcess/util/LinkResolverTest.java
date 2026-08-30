/*
 * Copyright (c) 2026 Sachin Arunkumar
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
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

class LinkResolverTest extends AbstractBaseTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "linked.accdb",
        "../outside.accdb",
        "..\\outside.accdb",
        "C:\\data\\linked.accdb",
        "\\\\server\\share\\linked.accdb",
        "//server/share/linked.accdb",
        "\\\\?\\UNC\\server\\share\\linked.accdb",
        "\\\\.\\linked.accdb",
        "\\/server/share/linked.accdb"
    })
    void testDefaultRejectsAutomaticLinks(String linkedDbName) throws IOException {
        try (Database linkerDb = createDbMem(FileFormat.V2010)) {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> LinkResolver.DEFAULT.resolveLinkedDatabase(linkerDb, linkedDbName));

            assertEquals(linkedDbName, ex.getFile());
        }
    }

    @Test
    void testUnrestrictedAllowsLocalPath() throws IOException {
        File linkeeFile;
        try (Database linkeeDb = createDb(FileFormat.V2010, false, false)) {
            linkeeFile = linkeeDb.getFile();
        }

        try (Database linkerDb = createDbMem(FileFormat.V2010);
             Database resolvedDb = LinkResolver.UNRESTRICTED.resolveLinkedDatabase(linkerDb, linkeeFile.getPath())) {
            assertEquals(linkeeFile.getCanonicalFile(), resolvedDb.getFile().getCanonicalFile());
        }
    }

    @Test
    void testLinkedTableDoesNotOpenNetworkPathByDefault() throws IOException {
        String linkedDbName = "\\\\server\\share\\linked.accdb";

        try (Database db = createDbMem(FileFormat.V2010)) {
            db.createLinkedTable("RemoteTable", linkedDbName, "Table1");

            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> db.getTable("RemoteTable"));
            assertEquals(linkedDbName, ex.getFile());
            assertTrue(db.getLinkedDatabases().isEmpty());
        }
    }
}
