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
package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.INDEX;
import static io.github.spannm.jackcess.test.TestUtil.assertTable;
import static io.github.spannm.jackcess.test.TestUtil.createExpectedRow;
import static io.github.spannm.jackcess.test.TestUtil.createExpectedTable;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Cursor;
import io.github.spannm.jackcess.CursorBuilder;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FKEnforcerTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void noEnforceForeignKeys(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            db.setEnforceForeignKeys(false);
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            t1.addRow(20, 0, 20, "some data", 20);
            assertThat(t1.getRowCount()).isGreaterThan(0);

            Cursor c = CursorBuilder.createCursor(t2);
            c.moveToNextRow();
            c.updateCurrentRow(30, "foo30");
            assertThat(c.getCurrentRow()).containsValues(30, "foo30");

            c = CursorBuilder.createCursor(t3);
            c.moveToNextRow();
            int rowCountBeforeDelete = t3.getRowCount();
            c.deleteCurrentRow();
            assertThat(t3.getRowCount()).isEqualTo(rowCountBeforeDelete - 1);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void enforceForeignKeys(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            db.setEvaluateExpressions(false);
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            Map<ThrowingCallable, String> tests = new LinkedHashMap<>();
            tests.put(() -> t1.addRow(20, 0, 20, "some data", 20), "Table1[otherfk2]");
            tests.put(() -> {
                Cursor c = CursorBuilder.createCursor(t2);
                c.moveToNextRow();
                c.updateCurrentRow(30, "foo30");
            }, "Table2[id]");
            tests.put(() -> {
                Cursor c = CursorBuilder.createCursor(t3);
                c.moveToNextRow();
                c.deleteCurrentRow();
            }, "Table3[id]");
            tests.forEach((key, value) -> {
                IOException ex = catchThrowableOfType(key, IOException.class);
                assertThat(ex.getMessage()).contains(value);

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

            assertThat(t1.getRowCount()).isEqualTo(1);
        }
    }

    private static Row createT1Row(int id1, Integer fk1, Integer fk2, String data, Integer fk3) {
        return createExpectedRow("id", id1, "otherfk1", fk1, "otherfk2", fk2,
            "data", data, "otherfk3", fk3);
    }
}
