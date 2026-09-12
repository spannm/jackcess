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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
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
    void exportAllVariants() throws Exception {
        try (Database db = createTestDb()) {
            File dir = TestUtil.createTempDir("export1");
            ExportUtil.exportAll(db, dir);
            assertThat(new File(dir, "test.csv").isFile()).isTrue();

            File dir2 = TestUtil.createTempDir("export2");
            ExportUtil.exportAll(db, dir2, "txt", true);
            File exported = new File(dir2, "test.txt");
            assertThat(exported.isFile()).isTrue();

            File dir3 = TestUtil.createTempDir("export3");
            ExportUtil.exportAll(db, dir3, "dat", true, ";", '\'', SimpleExportFilter.INSTANCE);
            assertThat(new File(dir3, "test.dat").isFile()).isTrue();

            File dir4 = TestUtil.createTempDir("export4");
            new ExportUtil.Builder(db)
                .withFileNameExtension("out")
                .withHeader(true)
                .withDelimiter(",")
                .withQuote('"')
                .withFilter(SimpleExportFilter.INSTANCE)
                .exportAll(dir4);
            assertThat(new File(dir4, "test.out").isFile()).isTrue();
        }
    }

    @Test
    void exportFileAndWriterVariants() throws Exception {
        try (Database db = createTestDb()) {
            File f = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            ExportUtil.exportFile(db, "test", f);
            assertThat(f.length() > 0).isTrue();

            File f2 = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            ExportUtil.exportFile(db, "test", f2, true, ";", '"', SimpleExportFilter.INSTANCE);
            assertThat(f2.length() > 0).isTrue();

            File f3 = TestUtil.createTempFile(getShortTestMethodName(), ".csv", false);
            new ExportUtil.Builder(db).withTableName("test").exportFile(f3);
            assertThat(f3.length() > 0).isTrue();

            StringWriter sw = new StringWriter();
            ExportUtil.exportWriter(db, "test", new BufferedWriter(sw));
            assertThat(sw.toString().contains("a")).isTrue();

            // export from an explicit cursor via builder
            StringWriter sw2 = new StringWriter();
            Cursor cursor = CursorBuilder.createCursor(db.getTable("test"));
            new ExportUtil.Builder(cursor).withHeader(true).exportWriter(new BufferedWriter(sw2));
            assertThat(sw2.toString().contains("col1")).isTrue();

            // builder using the "no-arg-ish" constructors
            StringWriter sw3 = new StringWriter();
            new ExportUtil.Builder(db, null)
                .withDatabase(db)
                .withTableName("test")
                .withCursor(null)
                .exportWriter(new BufferedWriter(sw3));
            assertThat(sw3.toString().contains("b")).isTrue();
        }
    }

    @Test
    void exportWithColumnFilter() throws Exception {
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
            assertThat(out.contains("col1")).isTrue();
            assertThat(out.contains("col2")).isFalse();
        }
    }

    @Test
    void importFileVariants() throws Exception {
        File sample = new File(DIR_TEST_DATA, "sample-input.tab");
        try (Database db = createDbMem(FileFormat.V2000)) {
            String n1 = ImportUtil.importFile(sample, db, "imp1", "\\t");
            assertThat(db.getTable(n1)).isNotNull();

            String n2 = ImportUtil.importFile(sample, db, "imp2", "\\t", SimpleImportFilter.INSTANCE);
            assertThat(db.getTable(n2)).isNotNull();

            String n3 = ImportUtil.importFile(sample, db, "imp3", "\\t", '"',
                SimpleImportFilter.INSTANCE, false);
            assertThat(db.getTable(n3)).isNotNull();

            // importing again under an existing name creates a uniquely named table
            String n4 = ImportUtil.importFile(sample, db, "imp1", "\\t");
            assertThat(n4).isNotEqualTo(n1);
            assertThat(db.getTable(n4)).isNotNull();
        }
    }

    @Test
    void importReaderVariants() throws Exception {
        String data = "c1\tc2\nv1\tv2\n";
        try (Database db = createDbMem(FileFormat.V2000)) {
            String n1 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r1", "\\t");
            assertThat(TestUtil.countRows(db.getTable(n1))).isEqualTo(1);

            String n2 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r2", "\\t",
                SimpleImportFilter.INSTANCE);
            assertThat(db.getTable(n2)).isNotNull();

            String n3 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r3", "\\t",
                SimpleImportFilter.INSTANCE, false);
            assertThat(db.getTable(n3)).isNotNull();

            String n4 = ImportUtil.importReader(new BufferedReader(new StringReader(data)), db, "r4", "\\t", '"',
                SimpleImportFilter.INSTANCE, false);
            assertThat(db.getTable(n4)).isNotNull();

            // append to an existing table without a header line
            String n5 = ImportUtil.importReader(new BufferedReader(new StringReader("v3\tv4\n")), db, n1, "\\t", '"',
                SimpleImportFilter.INSTANCE, true, false);
            assertThat(n5).isEqualTo(n1);
            assertThat(TestUtil.countRows(db.getTable(n1))).isEqualTo(2);

            // empty input
            assertThat(ImportUtil.importReader(new BufferedReader(new StringReader("")), db, "rx", "\\t")).isNull();

            // unterminated quoted value
            assertThrows(EOFException.class, () -> ImportUtil.importReader(
                new BufferedReader(new StringReader("c1\tc2\n\"unterminated\tvalue\n")), db, "ry", "\\t"));
        }
    }

    @Test
    void importBuilder() throws Exception {
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
            assertThat(name).isEqualTo("b1");

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
            assertThat(TestUtil.countRows(db.getTable(rsName))).isEqualTo(2);

            // re-import into the existing table
            rs.reset();
            String rsName2 = new ImportUtil.Builder(db, "fromRs")
                .withUseExistingTable(true)
                .importResultSet(rs.toResultSet());
            assertThat(rsName2).isEqualTo(rsName);
            assertThat(TestUtil.countRows(db.getTable(rsName))).isEqualTo(5);
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
