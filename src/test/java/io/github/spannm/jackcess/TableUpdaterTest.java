package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.DatabaseBuilder.*;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.*;

class TableUpdaterTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testTableUpdating(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, false, true, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testTableUpdatingOneToOne(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, true, true, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testTableUpdatingNoEnforce(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            doTestUpdating(db, false, false, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testTableUpdatingNamedRelationship(FileFormat fileFormat) throws IOException {
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

        assertEquals(Objects.requireNonNullElse(relationshipName, "TestTableTestTable2"), rel.getName());
        assertSame(t1, rel.getFromTable());
        assertEquals(List.of(t1.getColumn("id")), rel.getFromColumns());
        assertSame(t2, rel.getToTable());
        assertEquals(List.of(t2.getColumn("id2")), rel.getToColumns());
        assertEquals(oneToOne, rel.isOneToOne());
        assertEquals(enforce, rel.hasReferentialIntegrity());
        assertEquals(enforce, rel.cascadeDeletes());
        assertFalse(rel.cascadeUpdates());
        assertEquals(Relationship.JoinType.INNER, rel.getJoinType());

        assertEquals(t1idxs, t1.getIndexes().size());
        assertEquals(1, ((TableImpl) t1).getIndexDatas().size());

        assertEquals(t2idxs, t2.getIndexes().size());
        assertEquals(t2idxs > 0 ? 1 : 0, ((TableImpl) t2).getIndexDatas().size());

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
            assertEquals(id, r.get("id"));
            id++;
            if (id == 5) {
                id++;
            }
        }

        id = 0;
        for (Row r : t2) {
            assertEquals(id, r.get("id2"));
            id++;
            if (enforce && id == 5) {
                id++;
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testInvalidUpdate(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            Table t1 = newTable("TestTable")
                .addColumn(newColumn("id", DataType.LONG))
                .toTable(db);

            try {
                newColumn("ID", DataType.TEXT)
                    .addToTable(t1);
                fail("created table with no columns?");
            } catch (IllegalArgumentException e) {
                // success
            }

            Table t2 = newTable("TestTable2")
                .addColumn(newColumn("id2", DataType.LONG))
                .toTable(db);

            try {
                newRelationship(t1, t2)
                    .toRelationship(db);
                fail("created rel with no columns?");
            } catch (IllegalArgumentException e) {
                // success
            }

            try {
                newRelationship("TestTable", "TestTable2")
                    .addColumns("id", "id")
                    .toRelationship(db);
                fail("created rel with wrong columns?");
            } catch (IllegalArgumentException e) {
                // success
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testUpdateLargeTableDef(FileFormat fileFormat) throws IOException {
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
            assertEquals(expectedRowData, t.getNextRow());
        }
    }
}
