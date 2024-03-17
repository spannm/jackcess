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

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

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
    @FileFormatSource
    void testErrorHandler(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table =
                new TableBuilder("test")
                    .addColumn(new ColumnBuilder("col", DataType.TEXT))
                    .addColumn(new ColumnBuilder("val", DataType.LONG))
                    .toTable(db);

            table.addRow("row1", 1);
            table.addRow("row2", 2);
            table.addRow("row3", 3);

            TestUtil.assertTable(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("col", "row1", "val", 1),
                TestUtil.createExpectedRow("col", "row2", "val", 2),
                TestUtil.createExpectedRow("col", "row3", "val", 3)),
                table);

            replaceColumn(table, "val");

            table.reset();

            assertThrows(IOException.class, table::getNextRow);

            table.reset();
            table.setErrorHandler(new ReplacementErrorHandler());

            TestUtil.assertTable(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("col", "row1", "val", null),
                TestUtil.createExpectedRow("col", "row2", "val", null),
                TestUtil.createExpectedRow("col", "row3", "val", null)),
                table);

            Cursor c1 = CursorBuilder.createCursor(table);
            Cursor c2 = CursorBuilder.createCursor(table);
            Cursor c3 = CursorBuilder.createCursor(table);

            c2.setErrorHandler(new DebugErrorHandler("#error"));
            c3.setErrorHandler(ErrorHandler.DEFAULT);

            TestUtil.assertCursor(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("col", "row1", "val", null),
                TestUtil.createExpectedRow("col", "row2", "val", null),
                TestUtil.createExpectedRow("col", "row3", "val", null)),
                c1);

            TestUtil.assertCursor(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("col", "row1", "val", "#error"),
                TestUtil.createExpectedRow("col", "row2", "val", "#error"),
                TestUtil.createExpectedRow("col", "row3", "val", "#error")),
                c2);

            assertThrows(IOException.class, c3::getNextRow);

            table.setErrorHandler(null);
            c1.setErrorHandler(null);
            c1.reset();

            assertThrows(IOException.class, c1::getNextRow);
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
