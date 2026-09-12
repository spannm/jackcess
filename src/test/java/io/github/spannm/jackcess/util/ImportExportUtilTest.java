/*
 * Copyright (c) 2025 Markus Spann
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
package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Additional tests for {@link ExportUtil} and {@link ImportUtil}.
 */
class ImportExportUtilTest extends AbstractBaseTest {

    private Database createTestDb() throws IOException {
        Database db = createDbMem(FileFormat.V2000);
        Table t = new TableBuilder("test")
            .addColumn(new ColumnBuilder("col1", DataType.TEXT))
            .addColumn(new ColumnBuilder("col2", DataType.LONG))
            .toTable(db);
        t.addRow("a", 1);
        t.addRow("b", 2);
        return db;
    }

    @Test
    void testExportAllVariants() throws IOException {
        try (Database db = createTestDb()) {
            File dir = TestUtil.createTempDir("export1");
            ExportUtil.exportAll(db, dir);
            assertTrue(new File(dir, "test.csv").isFile());

            File dir2 = TestUtil.createTempDir("export2");
            ExportUtil.exportAll(db, dir2, "txt", true);
            File exported = new File(dir2, "test.txt");
            assertTrue(exported.isFile());

            File dir3 = TestUtil.createTempDir("export3");
            ExportUtil.exportAll(db, dir3, "dat", true, ";", '\'', SimpleExportFilter.INSTANCE);
            assertTrue(new File(dir3, "test.dat").isFile());

            File dir4 = TestUtil.createTempDir("export4");
            new ExportUtil.Builder(db)
                .withFileNameExtension("out")
                .withHeader(true)
                .withDelimiter(",")
                .withQuote('"')
                .withFilter(SimpleExportFilter.INSTANCE)
                .exportAll(dir4);
            assertTrue(new File(dir4, "test.out").isFile());
        }
    }

    @Test
    void testExportFileAndWriterVariants() throws IOException {
        try (Database db = createTestDb()) {
            File f = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            ExportUtil.exportFile(db, "test", f);
            assertTrue(f.length() > 0);

            File f2 = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            ExportUtil.exportFile(db, "test", f2, true, ";", '"', SimpleExportFilter.INSTANCE);
            assertTrue(f2.length() > 0);

            File f3 = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            new ExportUtil.Builder(db).withTableName("test").exportFile(f3);
            assertTrue(f3.length() > 0);

            StringWriter sw = new StringWriter();
            ExportUtil.exportWriter(db, "test", new BufferedWriter(sw));
            assertTrue(sw.toString().contains("a"));

            // export from an explicit cursor via builder
            StringWriter sw2 = new StringWriter();
            Cursor cursor = CursorBuilder.createCursor(db.getTable("test"));
            new ExportUtil.Builder(cursor).withHeader(true).exportWriter(new BufferedWriter(sw2));
            assertTrue(sw2.toString().contains("col1"));

            // builder using the "no-arg-ish" constructors
            StringWriter sw3 = new StringWriter();
            new ExportUtil.Builder(db, null)
                .withDatabase(db)
                .withTableName("test")
                .withCursor(null)
                .exportWriter(new BufferedWriter(sw3));
            assertTrue(sw3.toString().contains("b"));
        }
    }

    @Test
    void testExportWithColumnFilter() throws IOException {
        try (Database db = createTestDb()) {
            ExportFilter filter = new SimpleExportFilter() {
                @Override
                public List<Column> filterColumns(List<Column> columns) {
                    List<Column> filtered = new ArrayList<>(columns);
                    filtered.remove(filtered.size() - 1);
                    return filtered;
                }
            };

            StringWriter sw = new StringWriter();
            ExportUtil.exportWriter(CursorBuilder.createCursor(db.getTable("test")),
                new BufferedWriter(sw), true, null, '"', filter);
            String out = sw.toString();
            assertTrue(out.contains("col1"));
            assertFalse(out.contains("col2"));
        }
    }

    @Test
    void testImportFileVariants() throws IOException {
        File sample = new File(DIR_TEST_DATA, "sample-input.tab");
        try (Database db = createDbMem(FileFormat.V2000)) {
            String n1 = ImportUtil.importFile(sample, db, "imp1", "\\t");
            assertNotNull(db.getTable(n1));

            String n2 = ImportUtil.importFile(sample, db, "imp2", "\\t", SimpleImportFilter.INSTANCE);
            assertNotNull(db.getTable(n2));

            String n3 = ImportUtil.importFile(sample, db, "imp3", "\\t", '"',
                SimpleImportFilter.INSTANCE, false);
            assertNotNull(db.getTable(n3));

            // importing again under an existing name creates a uniquely named table
            String n4 = ImportUtil.importFile(sample, db, "imp1", "\\t");
            assertNotEquals(n1, n4);
            assertNotNull(db.getTable(n4));
        }
    }

    @Test
    void testImportReaderVariants() throws IOException {
        String data = "c1\tc2\nv1\tv2\n";
        try (Database db = createDbMem(FileFormat.V2000)) {
            String n1 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r1", "\\t");
            assertEquals(1, TestUtil.countRows(db.getTable(n1)));

            String n2 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r2", "\\t",
                SimpleImportFilter.INSTANCE);
            assertNotNull(db.getTable(n2));

            String n3 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r3", "\\t",
                SimpleImportFilter.INSTANCE, false);
            assertNotNull(db.getTable(n3));

            String n4 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r4", "\\t", '"',
                SimpleImportFilter.INSTANCE, false);
            assertNotNull(db.getTable(n4));

            // append to an existing table without a header line
            String n5 = ImportUtil.importReader(new BufferedReader(new StringReader("v3\tv4\n")), db, n1, "\\t", '"',
                SimpleImportFilter.INSTANCE, true, false);
            assertEquals(n1, n5);
            assertEquals(2, TestUtil.countRows(db.getTable(n1)));

            // empty input
            assertNull(ImportUtil.importReader(new BufferedReader(new StringReader("")), db, "rx", "\\t"));

            // unterminated quoted value
            assertThrows(EOFException.class, () -> ImportUtil.importReader(
                new BufferedReader(new StringReader("c1\tc2\n\"unterminated\tvalue\n")), db, "ry", "\\t"));
        }
    }

    @Test
    void testImportBuilder() throws IOException, SQLException {
        String data = "c1;c2\n'v1';v2\n";
        try (Database db = createDbMem(FileFormat.V2000)) {
            String name = new ImportUtil.Builder(db)
                .withDatabase(db)
                .withTableName("b1")
                .withDelimiter(";")
                .withQuote('\'')
                .withFilter(SimpleImportFilter.INSTANCE)
                .withUseExistingTable(false)
                .withHeader(true)
                .importReader(new BufferedReader(new StringReader(data)));
            assertEquals("b1", name);

            TestResultSet rs = new TestResultSet();
            rs.addColumn(Types.INTEGER, "num");
            rs.addColumn(Types.VARCHAR, "txt", 60, 0, 0);
            rs.addRow(1, "one");
            rs.addRow(2, "two");
            rs.addRow(3, "three");

            ImportFilter oddFilter = new SimpleImportFilter() {
                private int num;

                @Override
                public Object[] filterRow(Object[] row) {
                    return num++ % 2 == 1 ? null : row;
                }
            };

            String rsName = new ImportUtil.Builder(db, "fromRs")
                .withFilter(oddFilter)
                .importResultSet(rs.toResultSet());
            assertEquals(2, TestUtil.countRows(db.getTable(rsName)));

            // re-import into the existing table
            rs.reset();
            String rsName2 = new ImportUtil.Builder(db, "fromRs")
                .withUseExistingTable(true)
                .importResultSet(rs.toResultSet());
            assertEquals(rsName, rsName2);
            assertEquals(5, TestUtil.countRows(db.getTable(rsName)));
        }
    }

    /**
     * Simple {@link ResultSet} stand-in which returns a fixed set of rows.
     */
    private static final class TestResultSet implements InvocationHandler {
        private final List<Integer>  types        = new ArrayList<>();
        private final List<String>   names        = new ArrayList<>();
        private final List<Integer>  displaySizes = new ArrayList<>();
        private final List<Integer>  scales       = new ArrayList<>();
        private final List<Integer>  precisions   = new ArrayList<>();
        private final List<Object[]> rows         = new ArrayList<>();
        private int                  rowIdx       = -1;

        ResultSet toResultSet() {
            return (ResultSet) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {ResultSet.class}, this);
        }

        void reset() {
            rowIdx = -1;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getMetaData":
                    return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] {ResultSetMetaData.class}, this);
                case "next":
                    rowIdx++;
                    return rowIdx < rows.size();
                case "getObject":
                    return rows.get(rowIdx)[(Integer) args[0] - 1];
                case "getColumnCount":
                    return types.size();
                case "getColumnName":
                case "getColumnLabel":
                    return getValue(names, args[0]);
                case "getColumnDisplaySize":
                    return getValue(displaySizes, args[0]);
                case "getColumnType":
                    return getValue(types, args[0]);
                case "getScale":
                    return getValue(scales, args[0]);
                case "getPrecision":
                    return getValue(precisions, args[0]);
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }

        void addColumn(int type, String name) {
            addColumn(type, name, 0, 0, 0);
        }

        void addColumn(int type, String name, int displaySize, int scale, int precision) {
            types.add(type);
            names.add(name);
            displaySizes.add(displaySize);
            scales.add(scale);
            precisions.add(precision);
        }

        void addRow(Object... values) {
            rows.add(values);
        }

        private static <T> T getValue(List<T> values, Object index) {
            return values.get((Integer) index - 1);
        }
    }

}
