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

import static io.github.spannm.jackcess.DatabaseBuilder.newColumn;
import static io.github.spannm.jackcess.DatabaseBuilder.newPrimaryKey;
import static io.github.spannm.jackcess.DatabaseBuilder.newRelationship;
import static io.github.spannm.jackcess.DatabaseBuilder.newTable;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
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

        assertThat(t1.getIndexes()).hasSize(t1idxs);
        assertThat(((TableImpl) t1).getIndexDatas()).hasSize(1);

        assertThat(t2.getIndexes()).hasSize(t2idxs);
        assertThat(((TableImpl) t2).getIndexDatas()).hasSize(t2idxs > 0 ? 1 : 0);

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
            assertThatThrownBy(() -> t2.addRow(10, "row10", "row-data10")).isInstanceOf(ConstraintViolationException.class);
        } else {
            assertThatCode(() -> t2.addRow(10, "row10", "row-data10")).doesNotThrowAnyException();
        }

        Row r1 = CursorBuilder.findRowByPrimaryKey(t1, 5);
        t1.deleteRow(r1);

        int id = 0;
        for (Row r : t1) {
            assertThat(r).containsEntry("id", id);
            id++;
            if (id == 5) {
                id++;
            }
        }

        id = 0;
        for (Row r : t2) {
            assertThat(r).containsEntry("id2", id);
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

            ColumnBuilder dupeIdCol = newColumn("ID", DataType.TEXT);
            assertThatThrownBy(() -> dupeIdCol.addToTable(t1)).isInstanceOf(IllegalArgumentException.class);

            Table t2 = newTable("TestTable2")
                .addColumn(newColumn("id2", DataType.LONG))
                .toTable(db);

            RelationshipBuilder noColsRel = newRelationship(t1, t2);
            assertThatThrownBy(() -> noColsRel.toRelationship(db)).isInstanceOf(IllegalArgumentException.class);

            RelationshipBuilder wrongColsRel = newRelationship("TestTable", "TestTable2")
                .addColumns("id", "id");
            assertThatThrownBy(() -> wrongColsRel.toRelationship(db)).isInstanceOf(IllegalArgumentException.class);
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
            RelationshipBuilder cycleRel = newRelationship(t3, t1).addColumns("id", "id")
                .withReferentialIntegrity().withCascadeDeletes();
            assertThatThrownBy(() -> cycleRel.toRelationship(db)).isInstanceOf(IllegalArgumentException.class);

            // a non-cascading relationship closing the same loop is fine, since it does not participate in cascading
            assertThatCode(
                () -> newRelationship(t3, t1).addColumns("id", "id")
                    .withReferentialIntegrity()
                    .toRelationship(db)).doesNotThrowAnyException();
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
            RelationshipBuilder selfCycleRel = newRelationship(t1, t1).addColumns("id", "parentId")
                .withReferentialIntegrity().withCascadeDeletes();
            assertThatThrownBy(() -> selfCycleRel.toRelationship(db)).isInstanceOf(IllegalArgumentException.class);

            // without cascading, a self-referencing relationship is fine
            assertThatCode(
                () -> newRelationship(t1, t1).addColumns("id", "parentId")
                    .withReferentialIntegrity()
                    .toRelationship(db)).doesNotThrowAnyException();
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
