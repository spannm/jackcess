package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.INDEX;
import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class FKEnforcerTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void testNoEnforceForeignKeys(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            db.setEnforceForeignKeys(false);
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            t1.addRow(20, 0, 20, "some data", 20);

            Cursor c = CursorBuilder.createCursor(t2);
            c.moveToNextRow();
            c.updateCurrentRow(30, "foo30");

            c = CursorBuilder.createCursor(t3);
            c.moveToNextRow();
            c.deleteCurrentRow();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void testEnforceForeignKeys(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            db.setEvaluateExpressions(false);
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            Map<Executable, String> tests = Map.of(
                () -> t1.addRow(20, 0, 20, "some data", 20), "Table1[otherfk2]",
                () -> {
                    Cursor c = CursorBuilder.createCursor(t2);
                    c.moveToNextRow();
                    c.updateCurrentRow(30, "foo30");
                }, "Table2[id]",
                () -> {
                    Cursor c = CursorBuilder.createCursor(t3);
                    c.moveToNextRow();
                    c.deleteCurrentRow();
                }, "Table3[id]");
            tests.forEach((key, value) -> {
                IOException ex = assertThrows(IOException.class, key);
                assertTrue(ex.getMessage().contains(value));

            });

            t1.addRow(21, null, null, "null fks", null);

            Cursor c = CursorBuilder.createCursor(t3);
            Column col = t3.getColumn("id");
            for (Row row : c) {
                int id = row.getInt("id");
                id += 20;
                c.setCurrentRowValue(col, id);
            }

            List<? extends Map<String, Object>> expectedRows =
                createExpectedTable(
                    createT1Row(0, 0, 30, "baz0", 0),
                    createT1Row(1, 1, 31, "baz11", 0),
                    createT1Row(2, 1, 31, "baz11-2", 0),
                    createT1Row(3, 2, 33, "baz13", 0),
                    createT1Row(21, null, null, "null fks", null));

            assertTable(expectedRows, t1);

            c = CursorBuilder.createCursor(t2);
            for (Iterator<?> iter = c.iterator(); iter.hasNext();) {
                iter.next();
                iter.remove();
            }

            assertEquals(1, t1.getRowCount());
        }
    }

    private static Row createT1Row(int id1, Integer fk1, Integer fk2, String data, Integer fk3) {
        return createExpectedRow("id", id1, "otherfk1", fk1, "otherfk2", fk2,
            "data", data, "otherfk3", fk3);
    }
}
