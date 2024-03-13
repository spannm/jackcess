/*
Copyright (c) 2016 James Ahlborn

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

package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.LINKED;
import static io.github.spannm.jackcess.test.Basename.LINKED_ODBC;
import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * @author James Ahlborn
 */
class LinkedTableTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(LINKED)
    void testLinkedTables(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            try {
                db.getTable("Table2");
                fail("FileNotFoundException should have been thrown");
            } catch (FileNotFoundException e) {
                // success
            }

            TableMetaData tmd = db.getTableMetaData("Table2");
            assertEquals("Table2", tmd.getName());
            assertTrue(tmd.isLinked());
            assertFalse(tmd.isSystem());
            assertEquals("Table1", tmd.getLinkedTableName());
            assertNull(tmd.getConnectionName());
            assertEquals(TableMetaData.Type.LINKED, tmd.getType());
            assertEquals("Z:\\jackcess_test\\linkeeTest.accdb", tmd.getLinkedDbName());
            assertNull(tmd.getTableDefinition(db));

            tmd = db.getTableMetaData("FooTable");
            assertNull(tmd);

            assertTrue(db.getLinkedDatabases().isEmpty());

            String linkeeDbName = "Z:\\jackcess_test\\linkeeTest.accdb";
            File linkeeFile = new File(DIR_TEST_DATA, "linkeeTest.accdb");
            db.setLinkResolver((linkerdb, dbName) -> {
                assertEquals(linkeeDbName, dbName);
                return DatabaseBuilder.open(linkeeFile);
            });

            Table t2 = db.getTable("Table2");

            assertEquals(1, db.getLinkedDatabases().size());
            Database linkeeDb = db.getLinkedDatabases().get(linkeeDbName);
            assertNotNull(linkeeDb);
            assertEquals(linkeeFile, linkeeDb.getFile());
            assertEquals("linkeeTest.accdb", ((DatabaseImpl) linkeeDb).getName());

            List<? extends Map<String, Object>> expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "ID", 1,
                        "Field1", "bar"));

            assertTable(expectedRows, t2);

            db.createLinkedTable("FooTable", linkeeDbName, "Table2");

            tmd = db.getTableMetaData("FooTable");
            assertEquals("FooTable", tmd.getName());
            assertTrue(tmd.isLinked());
            assertFalse(tmd.isSystem());
            assertEquals("Table2", tmd.getLinkedTableName());
            assertEquals("Z:\\jackcess_test\\linkeeTest.accdb", tmd.getLinkedDbName());

            Table t3 = db.getTable("FooTable");

            assertEquals(1, db.getLinkedDatabases().size());

            expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "ID", 1,
                        "Field1", "buzz"));

            assertTable(expectedRows, t3);

            tmd = db.getTableMetaData("Table1");
            assertEquals("Table1", tmd.getName());
            assertFalse(tmd.isLinked());
            assertFalse(tmd.isSystem());
            assertNull(tmd.getLinkedTableName());
            assertNull(tmd.getLinkedDbName());

            Table t1 = tmd.open(db);

            assertFalse(db.isLinkedTable(null));
            assertTrue(db.isLinkedTable(t2));
            assertTrue(db.isLinkedTable(t3));
            assertFalse(db.isLinkedTable(t1));

            List<Table> tables = DatabaseTest.getTables(db.newIterable());
            assertEquals(3, tables.size());
            assertTrue(tables.contains(t1));
            assertTrue(tables.contains(t2));
            assertTrue(tables.contains(t3));
            assertFalse(tables.contains(((DatabaseImpl) db).getSystemCatalog()));

            tables = DatabaseTest.getTables(db.newIterable().withIncludeNormalTables(false));
            assertEquals(2, tables.size());
            assertFalse(tables.contains(t1));
            assertTrue(tables.contains(t2));
            assertTrue(tables.contains(t3));
            assertFalse(tables.contains(((DatabaseImpl) db).getSystemCatalog()));

            tables = DatabaseTest.getTables(db.newIterable().withLocalUserTablesOnly());
            assertEquals(1, tables.size());
            assertTrue(tables.contains(t1));
            assertFalse(tables.contains(t2));
            assertFalse(tables.contains(t3));
            assertFalse(tables.contains(((DatabaseImpl) db).getSystemCatalog()));

            tables = DatabaseTest.getTables(db.newIterable().withSystemTablesOnly());
            assertTrue(tables.size() > 5);
            assertFalse(tables.contains(t1));
            assertFalse(tables.contains(t2));
            assertFalse(tables.contains(t3));
            assertTrue(tables.contains(((DatabaseImpl) db).getSystemCatalog()));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(LINKED_ODBC)
    void testOdbcLinkedTables(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            TableMetaData tmd = db.getTableMetaData("Ordrar");
            assertEquals(TableMetaData.Type.LINKED_ODBC, tmd.getType());
            assertEquals("dbo.Ordrar", tmd.getLinkedTableName());
            assertNull(tmd.getLinkedDbName());
            assertEquals("DSN=Magnapinna;Description=Safexit;UID=safexit;PWD=DummyPassword;APP=Microsoft Office;DATABASE=safexit", tmd.getConnectionName());
            assertFalse(tmd.toString().contains("DummyPassword"));

            TableDefinition t = tmd.getTableDefinition(db);

            List<? extends Column> cols = t.getColumns();
            assertEquals(20, cols.size());

            List<? extends Index> idxs = t.getIndexes();
            assertEquals(5, idxs.size());

            Table tbl = db.getTable("Ordrar");

            try {
                tbl.iterator();
                fail("UnsupportedOperationException should have been thrown");
            } catch (UnsupportedOperationException expected) {
                // expected
            }

            try {
                tbl.addRow(1L, "bar");
                fail("UnsupportedOperationException should have been thrown");
            } catch (UnsupportedOperationException expected) {
                // expected
            }
        }
    }

}
