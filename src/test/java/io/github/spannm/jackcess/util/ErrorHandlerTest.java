/*
Copyright (c) 2007 Health Market Science, Inc.

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
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.List;

/**
 * @author James Ahlborn
 */
class ErrorHandlerTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testErrorHandler(FileFormat fileFormat) throws Exception {
        try (Database db = create(fileFormat)) {
            Table table =
                new TableBuilder("test")
                    .addColumn(new ColumnBuilder("col", DataType.TEXT))
                    .addColumn(new ColumnBuilder("val", DataType.LONG))
                    .toTable(db);

            table.addRow("row1", 1);
            table.addRow("row2", 2);
            table.addRow("row3", 3);

            assertTable(createExpectedTable(
                createExpectedRow("col", "row1",
                    "val", 1),
                createExpectedRow("col", "row2",
                    "val", 2),
                createExpectedRow("col", "row3",
                    "val", 3)),
                table);

            replaceColumn(table, "val");

            table.reset();
            try {
                table.getNextRow();
                fail("IOException should have been thrown");
            } catch (IOException e) {
                // success
            }

            table.reset();
            table.setErrorHandler(new ReplacementErrorHandler());

            assertTable(createExpectedTable(
                createExpectedRow("col", "row1",
                    "val", null),
                createExpectedRow("col", "row2",
                    "val", null),
                createExpectedRow("col", "row3",
                    "val", null)),
                table);

            Cursor c1 = CursorBuilder.createCursor(table);
            Cursor c2 = CursorBuilder.createCursor(table);
            Cursor c3 = CursorBuilder.createCursor(table);

            c2.setErrorHandler(new DebugErrorHandler("#error"));
            c3.setErrorHandler(ErrorHandler.DEFAULT);

            assertCursor(createExpectedTable(
                createExpectedRow("col", "row1",
                    "val", null),
                createExpectedRow("col", "row2",
                    "val", null),
                createExpectedRow("col", "row3",
                    "val", null)),
                c1);

            assertCursor(createExpectedTable(
                createExpectedRow("col", "row1",
                    "val", "#error"),
                createExpectedRow("col", "row2",
                    "val", "#error"),
                createExpectedRow("col", "row3",
                    "val", "#error")),
                c2);

            try {
                c3.getNextRow();
                fail("IOException should have been thrown");
            } catch (IOException e) {
                // success
            }

            table.setErrorHandler(null);
            c1.setErrorHandler(null);
            c1.reset();
            try {
                c1.getNextRow();
                fail("IOException should have been thrown");
            } catch (IOException e) {
                // success
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceColumn(Table t, String colName) throws Exception {
        Field colsField = TableImpl.class.getDeclaredField("_columns");
        colsField.setAccessible(true);
        List<Column> cols = (List<Column>) colsField.get(t);

        Column srcCol = null;
        ColumnImpl destCol = new BogusColumn(t, colName);
        for (int i = 0; i < cols.size(); i++) {
            srcCol = cols.get(i);
            if (srcCol.getName().equals(colName)) {
                cols.set(i, destCol);
                break;
            }
        }

        // copy fields from source to dest
        for (Field f : Column.class.getDeclaredFields()) {
            if (!Modifier.isFinal(f.getModifiers())) {
                f.setAccessible(true);
                f.set(destCol, f.get(srcCol));
            }
        }

    }

    private static class BogusColumn extends ColumnImpl {
        private BogusColumn(Table table, String name) {
            super((TableImpl) table, name, DataType.LONG, 1, 0, 0);
        }

        @Override
        public Object read(byte[] data, ByteOrder order) throws IOException {
            throw new IOException("bogus column");
        }
    }

}
