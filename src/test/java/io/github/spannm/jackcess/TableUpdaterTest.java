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

import static io.github.spannm.jackcess.DatabaseBuilder.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static io.github.spannm.jackcess.test.Basename.DEL_COL;

import org.assertj.core.api.Assertions;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

final class TableUpdaterTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void tableUpdating(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, false, true, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void tableUpdatingOneToOne(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, true, true, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void tableUpdatingNoEnforce(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, false, false, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void tableUpdatingNamedRelationship(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, false, true, "FKnun3jvv47l9kyl74h85y8a0if");
        }
    }

    private void doTestUpdating(Database db, boolean oneToOne, boolean enforce, String relationshipName) throws IOException {
        Table t1 = newTable("TestTable")
            .addColumn(newColumn("id", DataType.LONG))
            .toTable(db);

        Table t2 = newTable("TestTable2")
            .addColumn(newColumn("id2", DataType.LONG))
            .toTable(db);

        int t1idxs = 1;
        newPrimaryKey("id").addToTable(t1);
        newColumn("data", DataType.TEXT).addToTable(t1);
        newColumn("bigdata", DataType.MEMO).addToTable(t1);

        newColumn("data2", DataType.TEXT).addToTable(t2);
        newColumn("bigdata2", DataType.MEMO).addToTable(t2);

        int t2idxs = 0;
        if (oneToOne) {
            t2idxs++;
            newPrimaryKey("id2").addToTable(t2);
        }

        RelationshipBuilder rb = newRelationship("TestTable", "TestTable2").addColumns("id", "id2");
        if (enforce) {
            t1idxs++;
            t2idxs++;
            rb.withReferentialIntegrity().withCascadeDeletes();
        }

        if (relationshipName != null) {
            rb.withName(relationshipName);
        }

        Relationship rel = rb.toRelationship(db);

        assertThat(rel.getName()).isEqualTo(relationshipName != null ? relationshipName : "TestTableTestTable2");
        assertThat(rel.getFromTable()).isSameAs(t1);
        assertThat(rel.getFromColumns()).isEqualTo(Arrays.asList(t1.getColumn("id")));
        assertThat(rel.getToTable()).isSameAs(t2);
        assertThat(rel.getToColumns()).isEqualTo(Arrays.asList(t2.getColumn("id2")));
        assertThat(rel.isOneToOne()).isEqualTo(oneToOne);
        assertThat(rel.hasReferentialIntegrity()).isEqualTo(enforce);
        assertThat(rel.cascadeDeletes()).isEqualTo(enforce);
        assertThat(rel.cascadeUpdates()).isFalse();
        assertThat(rel.getJoinType()).isEqualTo(Relationship.JoinType.INNER);

        assertThat(t1.getIndexes().size()).isEqualTo(t1idxs);
        assertThat(((TableImpl) t1).getIndexDatas().size()).isEqualTo(1);

        assertThat(t2.getIndexes().size()).isEqualTo(t2idxs);
        assertThat(((TableImpl) t2).getIndexDatas().size()).isEqualTo(t2idxs > 0 ? 1 : 0);

        ((DatabaseImpl) db).getPageChannel().startWrite();
        try {
            for (int i = 0; i < 10; i++) {
                t1.addRow(i, "row" + i, "row-data" + i);
            }

            for (int i = 0; i < 10; i++) {
                t2.addRow(i, "row2_" + i, "row-data2_" + i);
            }

        } finally {
            ((DatabaseImpl) db).getPageChannel().finishWrite();
        }

        if (enforce) {
            assertThrows(ConstraintViolationException.class, () -> t2.addRow(10, "row10", "row-data10"));
        } else {
            assertDoesNotThrow(() -> t2.addRow(10, "row10", "row-data10"));
        }

        Row r1 = CursorBuilder.findRowByPrimaryKey(t1, 5);
        t1.deleteRow(r1);

        int id = 0;
        for (Row r : t1) {
            assertThat(r.get("id")).isEqualTo(id);
            id++;
            if (id == 5) {
                id++;
            }
        }

        id = 0;
        for (Row r : t2) {
            assertThat(r.get("id2")).isEqualTo(id);
            id++;
            if (enforce && id == 5) {
                id++;
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void invalidUpdate(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table t1 = newTable("TestTable")
                .addColumn(newColumn("id", DataType.LONG))
                .toTable(db);

            try {
                newColumn("ID", DataType.TEXT)
                    .addToTable(t1);
                Assertions.fail("created table with no columns?");
            } catch (IllegalArgumentException _ex) {
                // success
            }

            Table t2 = newTable("TestTable2")
                .addColumn(newColumn("id2", DataType.LONG))
                .toTable(db);

            try {
                newRelationship(t1, t2)
                    .toRelationship(db);
                Assertions.fail("created rel with no columns?");
            } catch (IllegalArgumentException _ex) {
                // success
            }

            try {
                newRelationship("TestTable", "TestTable2")
                    .addColumns("id", "id")
                    .toRelationship(db);
                Assertions.fail("created rel with wrong columns?");
            } catch (IllegalArgumentException _ex) {
                // success
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void cascadeRelationshipCycleRejected(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table t1 = newTable("TestTable1")
                .addColumn(newColumn("id", DataType.LONG))
                .toTable(db);
            newPrimaryKey("id").addToTable(t1);

            Table t2 = newTable("TestTable2")
                .addColumn(newColumn("id", DataType.LONG))
                .toTable(db);
            newPrimaryKey("id").addToTable(t2);

            Table t3 = newTable("TestTable3")
                .addColumn(newColumn("id", DataType.LONG))
                .toTable(db);
            newPrimaryKey("id").addToTable(t3);

            // TestTable1 -(cascade delete)-> TestTable2
            newRelationship(t1, t2).addColumns("id", "id")
                .withReferentialIntegrity().withCascadeDeletes()
                .toRelationship(db);

            // TestTable2 -(cascade update)-> TestTable3
            newRelationship(t2, t3).addColumns("id", "id")
                .withReferentialIntegrity().withCascadeUpdates()
                .toRelationship(db);

            // closing the loop, TestTable3 -(cascade delete)-> TestTable1, would create an infinite cascade cycle
            assertThrows(IllegalArgumentException.class,
                () -> newRelationship(t3, t1).addColumns("id", "id")
                    .withReferentialIntegrity().withCascadeDeletes()
                    .toRelationship(db));

            // a non-cascading relationship closing the same loop is fine, since it does not participate in cascading
            assertDoesNotThrow(
                () -> newRelationship(t3, t1).addColumns("id", "id")
                    .withReferentialIntegrity()
                    .toRelationship(db));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void cascadeRelationshipSelfCycleRejected(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table t1 = newTable("TestTable1")
                .addColumn(newColumn("id", DataType.LONG))
                .addColumn(newColumn("parentId", DataType.LONG))
                .toTable(db);
            newPrimaryKey("id").addToTable(t1);

            // a table cascading to itself is a (degenerate) cascade cycle
            assertThrows(IllegalArgumentException.class,
                () -> newRelationship(t1, t1).addColumns("id", "parentId")
                    .withReferentialIntegrity().withCascadeDeletes()
                    .toRelationship(db));

            // without cascading, a self-referencing relationship is fine
            assertDoesNotThrow(
                () -> newRelationship(t1, t1).addColumns("id", "parentId")
                    .withReferentialIntegrity()
                    .toRelationship(db));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void updateLargeTableDef(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            final int numColumns = 89;

            Table t = newTable("test")
                .addColumn(newColumn("first", DataType.TEXT))
                .toTable(db);

            List<String> colNames = new ArrayList<>();
            colNames.add("first");
            for (int i = 0; i < numColumns; i++) {
                String colName = "MyColumnName" + i;
                colNames.add(colName);
                DataType type = i % 3 == 0 ? DataType.MEMO : DataType.TEXT;
                newColumn(colName, type)
                    .addToTable(t);
            }

            List<String> row = new ArrayList<>();
            Map<String, Object> expectedRowData = new LinkedHashMap<>();
            for (int i = 0; i < colNames.size(); i++) {
                String value = i + " some row data";
                row.add(value);
                expectedRowData.put(colNames.get(i), value);
            }

            t.addRow(row.toArray());

            t.reset();
            assertThat(t.getNextRow()).isEqualTo(expectedRowData);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(DEL_COL)
    void addColumn_tableWithDeletedColumns_getsUnusedId(TestDb testDb) throws Exception {
        // a new column takes an id above every id in use, rather than the physical column number, which in these
        // tables is below the highest id
        File dbFile;
        short maxId;
        try (Database db = testDb.openCopy()) {
            dbFile = db.getFile();
            TableImpl t = (TableImpl) db.getTable("Table1");

            maxId = (short) t.getColumns().stream().mapToInt(ColumnImpl::getColumnId).max().orElse(-1);
            // the deleted columns left the highest id past the last column
            assertThat(maxId).isGreaterThan((short) (t.getColumnCount() - 1));

            newColumn("newCol", DataType.TEXT).addToTable(t);
        }

        try (Database db = DatabaseBuilder.open(dbFile)) {
            TableImpl t = (TableImpl) db.getTable("Table1");

            assertThat(((ColumnImpl) t.getColumn("newCol")).getColumnId()).isEqualTo((short) (maxId + 1));

            Set<Short> ids = new HashSet<>();
            for (ColumnImpl col : t.getColumns()) {
                assertThat(ids.add(col.getColumnId())).as("duplicate column id %d", col.getColumnId()).isTrue();
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void createRow_corruptTableDef_rejectsWrite(FileFormat fileFormat) throws Exception {
        // variable length column count which does not cover all the variable length columns. Writing a row would run
        // off the end of the offset table. An older version of jackcess could write this count when adding a column
        checkCorruptTableDef(fileFormat, t -> setTableField(t, "maxVarColumnCount", (short) 1), "data2");

        // column count which does not cover all the columns. Writing a row would run off the end of the null mask
        checkCorruptTableDef(fileFormat, t -> setTableField(t, "maxColumnCount", (short) 1), "data1");

        // fixed length column which ends beyond the maximum row size. Writing a row would run off the end of the row
        // buffer
        checkCorruptTableDef(fileFormat, t -> setColumnField(t, "id", "mfixedDataOffset", 5000), "id");
    }

    /** Damages part of a table definition which has already been loaded. */
    @FunctionalInterface
    private interface TableDefDamager {
        /**
         * Damages the definition of the given table.
         *
         * @param t table to damage
         * @throws Exception if the table cannot be modified
         */
        void damage(Table t) throws Exception;
    }

    /**
     * Creates a table, damages its definition and verifies that rows can still be read but no longer written.
     *
     * @param fileFormat      file format of the database to create
     * @param damager         damages the loaded table definition
     * @param expectedColName name of the column the error message must mention
     */
    private void checkCorruptTableDef(FileFormat fileFormat, TableDefDamager damager, String expectedColName) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG))
                .addColumn(newColumn("data1", DataType.TEXT))
                .addColumn(newColumn("data2", DataType.TEXT))
                .toTable(db);

            damager.damage(t);

            // the table def is validated when the table is loaded, so re-run the validation now that the def has
            // been damaged
            Method m = TableImpl.class.getDeclaredMethod("validateColumnDefs");
            m.setAccessible(true);
            m.invoke(t);

            assertThat(t.getNextRow()).isNull();

            assertThatThrownBy(() -> t.addRow(1, "foo", "bar"))
                .isInstanceOf(JackcessException.class)
                .hasMessageContaining("Table definition is corrupt")
                .hasMessageContaining(expectedColName);
        }
    }

    /**
     * Sets a short field of the given table via reflection.
     *
     * @param t         table to modify
     * @param fieldName name of the field
     * @param value     new value
     */
    private static void setTableField(Table t, String fieldName, short value) throws ReflectiveOperationException {
        Field f = TableImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setShort(t, value);
    }

    /**
     * Sets an int field of a column of the given table via reflection.
     *
     * @param t         table containing the column
     * @param colName   name of the column
     * @param fieldName name of the field
     * @param value     new value
     */
    private static void setColumnField(Table t, String colName, String fieldName, int value) throws ReflectiveOperationException {
        Field f = ColumnImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(t.getColumn(colName), value);
    }
}
