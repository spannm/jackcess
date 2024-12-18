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
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ImportTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testImportFromFile(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            String tableName = new ImportUtil.Builder(db, "test")
                .withDelimiter("\\t")
                .importFile(new File(DIR_TEST_DATA, "sample-input.tab"));
            Table t = db.getTable(tableName);

            List<String> colNames = new ArrayList<>();
            for (Column c : t.getColumns()) {
                colNames.add(c.getName());
            }
            assertEquals(List.of("Test1", "Test2", "Test3"), colNames);

            List<? extends Map<String, Object>> expectedRows =
                TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow(
                        "Test1", "Foo",
                        "Test2", "Bar",
                        "Test3", "Ralph"),
                    TestUtil.createExpectedRow(
                        "Test1", "S",
                        "Test2", "Mouse",
                        "Test3", "Rocks"),
                    TestUtil.createExpectedRow(
                        "Test1", "",
                        "Test2", "Partial line",
                        "Test3", null),
                    TestUtil.createExpectedRow(
                        "Test1", " Quoted Value",
                        "Test2", " bazz ",
                        "Test3", " Really \"Crazy" + ImportUtil.LINE_SEPARATOR
                            + "value\""),
                    TestUtil.createExpectedRow(
                        "Test1", "buzz",
                        "Test2", "embedded\tseparator",
                        "Test3", "long"));
            TestUtil.assertTable(expectedRows, t);

            t = new TableBuilder("test2")
                .addColumn(new ColumnBuilder("T1", DataType.TEXT))
                .addColumn(new ColumnBuilder("T2", DataType.TEXT))
                .addColumn(new ColumnBuilder("T3", DataType.TEXT))
                .toTable(db);

            new ImportUtil.Builder(db, "test2")
                .withDelimiter("\\t")
                .withUseExistingTable(true)
                .withHeader(false)
                .importFile(new File(DIR_TEST_DATA, "sample-input.tab"));

            expectedRows =
                TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow(
                        "T1", "Test1",
                        "T2", "Test2",
                        "T3", "Test3"),
                    TestUtil.createExpectedRow(
                        "T1", "Foo",
                        "T2", "Bar",
                        "T3", "Ralph"),
                    TestUtil.createExpectedRow(
                        "T1", "S",
                        "T2", "Mouse",
                        "T3", "Rocks"),
                    TestUtil.createExpectedRow(
                        "T1", "",
                        "T2", "Partial line",
                        "T3", null),
                    TestUtil.createExpectedRow(
                        "T1", " Quoted Value",
                        "T2", " bazz ",
                        "T3", " Really \"Crazy" + ImportUtil.LINE_SEPARATOR + "value\""),
                    TestUtil.createExpectedRow(
                        "T1", "buzz",
                        "T2", "embedded\tseparator",
                        "T3", "long"));
            TestUtil.assertTable(expectedRows, t);

            ImportFilter oddFilter = new SimpleImportFilter() {
                private int _num;

                @Override
                public Object[] filterRow(Object[] row) {
                    if (_num++ % 2 == 1) {
                        return null;
                    }
                    return row;
                }
            };

            tableName = new ImportUtil.Builder(db, "test3")
                .withDelimiter("\\t")
                .withFilter(oddFilter)
                .importFile(new File(DIR_TEST_DATA, "sample-input.tab"));
            t = db.getTable(tableName);

            colNames = new ArrayList<>();
            for (Column c : t.getColumns()) {
                colNames.add(c.getName());
            }
            assertEquals(List.of("Test1", "Test2", "Test3"), colNames);

            expectedRows =
                TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow(
                        "Test1", "Foo",
                        "Test2", "Bar",
                        "Test3", "Ralph"),
                    TestUtil.createExpectedRow(
                        "Test1", "",
                        "Test2", "Partial line",
                        "Test3", null),
                    TestUtil.createExpectedRow(
                        "Test1", "buzz",
                        "Test2", "embedded\tseparator",
                        "Test3", "long"));
            TestUtil.assertTable(expectedRows, t);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testImportFromFileWithOnlyHeaders(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            String tableName = new ImportUtil.Builder(db, "test")
                .withDelimiter("\\t")
                .importFile(new File(DIR_TEST_DATA, "sample-input-only-headers.tab"));

            Table t = db.getTable(tableName);

            List<String> colNames = new ArrayList<>();
            for (Column c : t.getColumns()) {
                colNames.add(c.getName());
            }
            assertEquals(List.of(
                "RESULT_PHYS_ID", "FIRST", "MIDDLE", "LAST", "OUTLIER",
                "RANK", "CLAIM_COUNT", "PROCEDURE_COUNT",
                "WEIGHTED_CLAIM_COUNT", "WEIGHTED_PROCEDURE_COUNT"),
                colNames);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCopySqlHeaders(FileFormat fileFormat) throws IOException, SQLException {
        TestResultSet rs = new TestResultSet();

        rs.addColumn(Types.INTEGER, "col1");
        rs.addColumn(Types.VARCHAR, "col2", 60, 0, 0);
        rs.addColumn(Types.VARCHAR, "col3", 500, 0, 0);
        rs.addColumn(Types.BINARY, "col4", 128, 0, 0);
        rs.addColumn(Types.BINARY, "col5", 512, 0, 0);
        rs.addColumn(Types.NUMERIC, "col6", 0, 7, 15);
        rs.addColumn(Types.VARCHAR, "col7", Integer.MAX_VALUE, 0, 0);

        Database db = createDbMem(fileFormat);
        ImportUtil.importResultSet((ResultSet) Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {ResultSet.class},
            rs), db, "Test1");

        Table t = db.getTable("Test1");
        List<? extends Column> columns = t.getColumns();
        assertEquals(7, columns.size());

        Column c = columns.get(0);
        assertEquals("col1", c.getName());
        assertEquals(DataType.LONG, c.getType());

        c = columns.get(1);
        assertEquals("col2", c.getName());
        assertEquals(DataType.TEXT, c.getType());
        assertEquals(120, c.getLength());

        c = columns.get(2);
        assertEquals("col3", c.getName());
        assertEquals(DataType.MEMO, c.getType());
        assertEquals(0, c.getLength());

        c = columns.get(3);
        assertEquals("col4", c.getName());
        assertEquals(DataType.BINARY, c.getType());
        assertEquals(128, c.getLength());

        c = columns.get(4);
        assertEquals("col5", c.getName());
        assertEquals(DataType.OLE, c.getType());
        assertEquals(0, c.getLength());

        c = columns.get(5);
        assertEquals("col6", c.getName());
        assertEquals(DataType.NUMERIC, c.getType());
        assertEquals(17, c.getLength());
        assertEquals(7, c.getScale());
        assertEquals(15, c.getPrecision());

        c = columns.get(6);
        assertEquals("col7", c.getName());
        assertEquals(DataType.MEMO, c.getType());
        assertEquals(0, c.getLength());
    }

    private static class TestResultSet implements InvocationHandler {
        private final List<Integer> _types        = new ArrayList<>();
        private final List<String>  _names        = new ArrayList<>();
        private final List<Integer> _displaySizes = new ArrayList<>();
        private final List<Integer> _scales       = new ArrayList<>();
        private final List<Integer> _precisions   = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if (methodName.equals("getMetaData")) {
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {ResultSetMetaData.class}, this);
            } else if (methodName.equals("next")) {
                return Boolean.FALSE;
            } else if (methodName.equals("getColumnCount")) {
                return _types.size();
            } else if (methodName.equals("getColumnName") || methodName.equals("getColumnLabel")) {
                return getValue(_names, args[0]);
            } else if (methodName.equals("getColumnDisplaySize")) {
                return getValue(_displaySizes, args[0]);
            } else if (methodName.equals("getColumnType")) {
                return getValue(_types, args[0]);
            } else if (methodName.equals("getScale")) {
                return getValue(_scales, args[0]);
            } else if (methodName.equals("getPrecision")) {
                return getValue(_precisions, args[0]);
            } else {
                throw new UnsupportedOperationException(methodName);
            }
        }

        public void addColumn(int type, String name) {
            addColumn(type, name, 0, 0, 0);
        }

        public void addColumn(int type, String name, int displaySize,
            int scale, int precision) {
            _types.add(type);
            _names.add(name);
            _displaySizes.add(displaySize);
            _scales.add(scale);
            _precisions.add(precision);
        }

        private static <T> T getValue(List<T> values, Object index) {
            return values.get((Integer) index - 1);
        }
    }

}
