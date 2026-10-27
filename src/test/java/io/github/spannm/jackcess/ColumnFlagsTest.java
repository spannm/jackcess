/*
 * Copyright (c) 2026 James Ahlborn
 * Copyright (c) 2026 Markus Spann
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
package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.COMMON1;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;

/**
 * Tests the column flag bits which mark a column the engine maintains and hides, and a column which holds a Windows
 * security identifier.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class ColumnFlagsTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void isHidden_userTable_noFlagSet(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            for (Column col : db.getTable("Table1").getColumns()) {
                assertThat(col.isHidden()).as(col.getName()).isFalse();
                assertThat(col.isSecurityIdentifier()).as(col.getName()).isFalse();
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void isHidden_systemTables_allHiddenAndOnlySidColumnsMarked(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            // the system catalog is entirely engine maintained, and the two columns which hold a Windows SID are the
            // only ones marked as such
            checkSystemTable(db, "MSysObjects", "Owner");
            checkSystemTable(db, "MSysACEs", "SID");
        }
    }

    /**
     * Asserts that every column of the given system table is hidden and only the named one holds a security identifier.
     *
     * @param db         the database to read from
     * @param tableName  the name of the system table
     * @param sidColName the name of the only column expected to hold a security identifier
     */
    private static void checkSystemTable(Database db, String tableName, String sidColName) throws IOException {
        Table t = db.getSystemTable(tableName);
        assertThat(t).as(tableName).isNotNull();
        for (Column col : t.getColumns()) {
            String desc = String.format("%s.%s", tableName, col.getName());
            assertThat(col.isHidden()).as(desc).isTrue();
            assertThat(col.isSecurityIdentifier()).as(desc).isEqualTo(sidColName.equals(col.getName()));
        }
    }
}
