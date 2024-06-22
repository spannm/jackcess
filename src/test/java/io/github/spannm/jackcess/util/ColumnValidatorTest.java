/*
Copyright (c) 2014 James Ahlborn

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

import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.List;
import java.util.Map;

class ColumnValidatorTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testValidate(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            ColumnValidatorFactory initFact = db.getColumnValidatorFactory();
            assertNotNull(initFact);

            Table table1 = new TableBuilder("Test")
                .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
                .addColumn(new ColumnBuilder("data", DataType.TEXT))
                .addColumn(new ColumnBuilder("num", DataType.LONG))
                .withPrimaryKey("id")
                .toTable(db);

            for (Column col : table1.getColumns()) {
                assertSame(SimpleColumnValidator.INSTANCE, col.getColumnValidator());
            }

            int val = -1;
            for (int i = 1; i <= 3; i++) {
                table1.addRow(Column.AUTO_NUMBER, "row" + i, val++);
            }

            // force table to be reloaded
            ((DatabaseImpl) db).clearTableCache();

            final ColumnValidator cv = (col, v1) -> {
                Number num = (Number) v1;
                if (num == null || num.intValue() < 0) {
                    throw new IllegalArgumentException("not gonna happen");
                }
                return v1;
            };

            ColumnValidatorFactory fact = col -> {
                Table t = col.getTable();
                assertFalse(t.isSystem());
                if (!"Test".equals(t.getName())) {
                    return null;
                }

                if (col.getType() == DataType.LONG) {
                    return cv;
                }

                return null;
            };

            db.setColumnValidatorFactory(fact);

            Table table2 = db.getTable("Test");

            for (Column col : table2.getColumns()) {
                ColumnValidator cur = col.getColumnValidator();
                assertNotNull(cur);
                if ("num".equals(col.getName())) {
                    assertSame(cv, cur);
                } else {
                    assertSame(SimpleColumnValidator.INSTANCE, cur);
                }
            }

            Column idCol = table2.getColumn("id");
            Column dataCol = table2.getColumn("data");
            Column numCol = table2.getColumn("num");

            assertThrows(IllegalArgumentException.class, () -> idCol.setColumnValidator(cv));

            assertSame(SimpleColumnValidator.INSTANCE, idCol.getColumnValidator());

            assertThrows(IllegalArgumentException.class, () -> table2.addRow(Column.AUTO_NUMBER, "row4", -3));

            table2.addRow(Column.AUTO_NUMBER, "row4", 4);

            List<? extends Map<String, Object>> expectedRows =
                createExpectedTable(
                    createExpectedRow("id", 1, "data", "row1", "num", -1),
                    createExpectedRow("id", 2, "data", "row2", "num", 0),
                    createExpectedRow("id", 3, "data", "row3", "num", 1),
                    createExpectedRow("id", 4, "data", "row4", "num", 4));

            assertTable(expectedRows, table2);

            IndexCursor pkCursor = CursorBuilder.createPrimaryKeyCursor(table2);
            assertNotNull(pkCursor.findRowByEntry(1));

            pkCursor.setCurrentRowValue(dataCol, "row1_mod");

            assertEquals(createExpectedRow("id", 1, "data", "row1_mod", "num", -1),
                pkCursor.getCurrentRow());

            assertThrows(IllegalArgumentException.class, () -> pkCursor.setCurrentRowValue(numCol, -2));

            assertEquals(createExpectedRow("id", 1, "data", "row1_mod", "num", -1),
                pkCursor.getCurrentRow());

            Row row3 = CursorBuilder.findRowByPrimaryKey(table2, 3);

            row3.put("num", -2);

            assertThrows(IllegalArgumentException.class, () -> table2.updateRow(row3));

            assertEquals(createExpectedRow("id", 3, "data", "row3", "num", 1),
                CursorBuilder.findRowByPrimaryKey(table2, 3));

            final ColumnValidator cv2 = (col, v1) -> {
                Number num = (Number) v1;
                if (num == null || num.intValue() < 0) {
                    return 0;
                }
                return v1;
            };

            numCol.setColumnValidator(cv2);

            table2.addRow(Column.AUTO_NUMBER, "row5", -5);

            expectedRows =
                createExpectedTable(
                    createExpectedRow("id", 1, "data", "row1_mod", "num", -1),
                    createExpectedRow("id", 2, "data", "row2", "num", 0),
                    createExpectedRow("id", 3, "data", "row3", "num", 1),
                    createExpectedRow("id", 4, "data", "row4", "num", 4),
                    createExpectedRow("id", 5, "data", "row5", "num", 0));

            assertTable(expectedRows, table2);

            assertNotNull(pkCursor.findRowByEntry(3));
            pkCursor.setCurrentRowValue(numCol, -10);

            assertEquals(createExpectedRow("id", 3, "data", "row3", "num", 0),
                pkCursor.getCurrentRow());
        }
    }
}
