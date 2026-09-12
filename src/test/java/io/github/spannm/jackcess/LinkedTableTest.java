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
package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.LINKED;
import static io.github.spannm.jackcess.test.Basename.LINKED_ODBC;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

class LinkedTableTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(LINKED)
    void linkedTables(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            assertThrows(AccessDeniedException.class, () -> db.getTable("Table2"));

            TableMetaData tmd = db.getTableMetaData("Table2");
            assertThat(tmd.getName()).isEqualTo("Table2");
            assertThat(tmd.isLinked()).isTrue();
            assertThat(tmd.isSystem()).isFalse();
            assertThat(tmd.getLinkedTableName()).isEqualTo("Table1");
            assertThat(tmd.getConnectionName()).isNull();
            assertThat(tmd.getType()).isEqualTo(TableMetaData.Type.LINKED);
            assertThat(tmd.getLinkedDbName()).isEqualTo("Z:\\jackcess_test\\linkeeTest.accdb");
            assertThat(tmd.getTableDefinition(db)).isNull();

            tmd = db.getTableMetaData("FooTable");
            assertThat(tmd).isNull();

            assertThat(db.getLinkedDatabases().isEmpty()).isTrue();

            String linkeeDbName = "Z:\\jackcess_test\\linkeeTest.accdb";
            File linkeeFile = new File(DIR_TEST_DATA, "linkeeTest.accdb");
            db.setLinkResolver((linkerdb, dbName) -> {
                assertThat(dbName).isEqualTo(linkeeDbName);
                return DatabaseBuilder.open(linkeeFile);
            });

            Table t2 = db.getTable("Table2");

            assertThat(db.getLinkedDatabases().size()).isEqualTo(1);
            Database linkeeDb = db.getLinkedDatabases().get(linkeeDbName);
            assertThat(linkeeDb).isNotNull();
            assertThat(linkeeDb.getFile()).isEqualTo(linkeeFile);
            assertThat(((DatabaseImpl) linkeeDb).getName()).isEqualTo("linkeeTest.accdb");

            List<? extends Map<String, Object>> expectedRows =
                TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow(
                        "ID", 1,
                        "Field1", "bar"));

            TestUtil.assertTable(expectedRows, t2);

            db.createLinkedTable("FooTable", linkeeDbName, "Table2");

            tmd = db.getTableMetaData("FooTable");
            assertThat(tmd.getName()).isEqualTo("FooTable");
            assertThat(tmd.isLinked()).isTrue();
            assertThat(tmd.isSystem()).isFalse();
            assertThat(tmd.getLinkedTableName()).isEqualTo("Table2");
            assertThat(tmd.getLinkedDbName()).isEqualTo("Z:\\jackcess_test\\linkeeTest.accdb");

            Table t3 = db.getTable("FooTable");

            assertThat(db.getLinkedDatabases().size()).isEqualTo(1);

            expectedRows =
                TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow(
                        "ID", 1,
                        "Field1", "buzz"));

            TestUtil.assertTable(expectedRows, t3);

            tmd = db.getTableMetaData("Table1");
            assertThat(tmd.getName()).isEqualTo("Table1");
            assertThat(tmd.isLinked()).isFalse();
            assertThat(tmd.isSystem()).isFalse();
            assertThat(tmd.getLinkedTableName()).isNull();
            assertThat(tmd.getLinkedDbName()).isNull();

            Table t1 = tmd.open(db);

            assertThat(db.isLinkedTable(null)).isFalse();
            assertThat(db.isLinkedTable(t2)).isTrue();
            assertThat(db.isLinkedTable(t3)).isTrue();
            assertThat(db.isLinkedTable(t1)).isFalse();

            List<Table> tables = DatabaseTest.getTables(db.newIterable());
            assertThat(tables.size()).isEqualTo(3);
            assertThat(tables.contains(t1)).isTrue();
            assertThat(tables.contains(t2)).isTrue();
            assertThat(tables.contains(t3)).isTrue();
            assertThat(tables.contains(((DatabaseImpl) db).getSystemCatalog())).isFalse();

            tables = DatabaseTest.getTables(db.newIterable().withIncludeNormalTables(false));
            assertThat(tables.size()).isEqualTo(2);
            assertThat(tables.contains(t1)).isFalse();
            assertThat(tables.contains(t2)).isTrue();
            assertThat(tables.contains(t3)).isTrue();
            assertThat(tables.contains(((DatabaseImpl) db).getSystemCatalog())).isFalse();

            tables = DatabaseTest.getTables(db.newIterable().withLocalUserTablesOnly());
            assertThat(tables.size()).isEqualTo(1);
            assertThat(tables.contains(t1)).isTrue();
            assertThat(tables.contains(t2)).isFalse();
            assertThat(tables.contains(t3)).isFalse();
            assertThat(tables.contains(((DatabaseImpl) db).getSystemCatalog())).isFalse();

            tables = DatabaseTest.getTables(db.newIterable().withSystemTablesOnly());
            assertThat(tables.size() > 5).isTrue();
            assertThat(tables.contains(t1)).isFalse();
            assertThat(tables.contains(t2)).isFalse();
            assertThat(tables.contains(t3)).isFalse();
            assertThat(tables.contains(((DatabaseImpl) db).getSystemCatalog())).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(LINKED_ODBC)
    void odbcLinkedTables(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            TableMetaData tmd = db.getTableMetaData("Ordrar");
            assertThat(tmd.getType()).isEqualTo(TableMetaData.Type.LINKED_ODBC);
            assertThat(tmd.getLinkedTableName()).isEqualTo("dbo.Ordrar");
            assertThat(tmd.getLinkedDbName()).isNull();
            assertThat(tmd.getConnectionName()).isEqualTo("DSN=Magnapinna;Description=Safexit;UID=safexit;PWD=DummyPassword;APP=Microsoft Office;DATABASE=safexit");
            assertThat(tmd.toString().contains("DummyPassword")).isFalse();

            TableDefinition t = tmd.getTableDefinition(db);

            List<? extends Column> cols = t.getColumns();
            assertThat(cols.size()).isEqualTo(20);

            List<? extends Index> idxs = t.getIndexes();
            assertThat(idxs.size()).isEqualTo(5);

            Table tbl = db.getTable("Ordrar");

            assertThrows(UnsupportedOperationException.class, tbl::iterator);

            assertThrows(UnsupportedOperationException.class, () -> tbl.addRow(1L, "bar"));
        }
    }

}
