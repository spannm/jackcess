/*
Copyright (c) 2015 James Ahlborn

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

package io.github.spannm.jackcess.test;

import static org.junit.jupiter.api.Assertions.*;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.complex.ComplexValueForeignKey;
import io.github.spannm.jackcess.impl.*;
import io.github.spannm.jackcess.util.MemFileChannel;

import java.io.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utilty code for the test cases.
 *
 * @author James Ahlborn
 */
@SuppressWarnings("deprecation")
public final class TestUtil {

    private static final File TEST_TEMP_DIR = createTempDir("test-jackcess-" + getCurrentUser());

    private TestUtil() {
    }

    private static Logger getLogger() {
        return System.getLogger(TestUtil.class.getName());
    }

    public static Database openDb(FileFormat fileFormat, File file) throws IOException {
        return openDb(fileFormat, file, false, null, true);
    }

    public static Database openDb(FileFormat fileFormat, File file, boolean inMem) throws IOException {
        return openDb(fileFormat, file, inMem, null, true);
    }

    public static Database openDb(FileFormat fileFormat, File file, boolean inMem, Charset charset) throws IOException {
        return openDb(fileFormat, file, inMem, charset, true);
    }

    public static Database openCopy(FileFormat fileFormat, File file) throws IOException {
        return openCopy(fileFormat, file, false);
    }

    public static Database openCopy(FileFormat fileFormat, File file, boolean keep) throws IOException {
        // split file name into prefix and suffix
        int fnLastDot = file.getName().lastIndexOf('.');
        File tempFile = TestUtil.createTempFile(file.getName().substring(0, fnLastDot), file.getName().substring(fnLastDot), keep);

        copyFile(file, tempFile);

        return openDb(fileFormat, tempFile, false, null, false);
    }

    static Database openDb(FileFormat fileFormat, File file, boolean inMem, Charset charset, boolean readOnly) throws IOException {
        FileChannel channel = inMem ? MemFileChannel.newChannel(file, MemFileChannel.RW_CHANNEL_MODE) : null;
        Database db = new DatabaseBuilder()
            .withFile(file)
            .withReadOnly(readOnly)
            .withAutoSync(AbstractBaseTest.getTestAutoSync())
            .withChannel(channel)
            .withCharset(charset)
            .open();
        if (fileFormat != null) {
            assertEquals(DatabaseImpl.getFileFormatDetails(fileFormat).getFormat(), ((DatabaseImpl) db).getFormat(), "Wrong JetFormat");
            assertEquals(fileFormat, db.getFileFormat(), "Wrong file format");
        }
        return db;
    }

    static Object[] createTestRow(String col1Val) {
        return new Object[] {col1Val, "R", "McCune", 1234, (byte) 0xad, 555.66d, 777.88f, (short) 999, new Date()};
    }

    public static Object[] createTestRow() {
        return createTestRow("Tim");
    }

    public static Map<String, Object> createTestRowMap(String col1Val) {
        return createExpectedRow("A", col1Val, "B", "R", "C", "McCune",
            "D", 1234, "E", (byte) 0xad, "F", 555.66d,
            "G", 777.88f, "H", (short) 999, "I", new Date());
    }

    public static void createTestTable(Database db) throws IOException {
        new TableBuilder("test")
            .addColumn(new ColumnBuilder("A", DataType.TEXT))
            .addColumn(new ColumnBuilder("B", DataType.TEXT))
            .addColumn(new ColumnBuilder("C", DataType.TEXT))
            .addColumn(new ColumnBuilder("D", DataType.LONG))
            .addColumn(new ColumnBuilder("E", DataType.BYTE))
            .addColumn(new ColumnBuilder("F", DataType.DOUBLE))
            .addColumn(new ColumnBuilder("G", DataType.FLOAT))
            .addColumn(new ColumnBuilder("H", DataType.INT))
            .addColumn(new ColumnBuilder("I", DataType.SHORT_DATE_TIME))
            .toTable(db);
    }

    public static String createString(int len) {
        return createString(len, 'a');
    }

    public static String createNonAsciiString(int len) {
        return createString(len, '\u0CC0');
    }

    private static String createString(int len, char firstChar) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) (firstChar + i % 26));
        }
        return sb.toString();
    }

    public static void assertRowCount(int expectedRowCount, Table table) throws IOException {
        assertEquals(expectedRowCount, countRows(table));
        assertEquals(expectedRowCount, table.getRowCount());
    }

    public static int countRows(Table table) throws IOException {
        Cursor cursor = CursorBuilder.createCursor(table);
        return (int) StreamSupport.stream(cursor.spliterator(), false).count();
    }

    public static void assertTable(List<? extends Map<String, Object>> expectedTable, Table table) throws IOException {
        assertCursor(expectedTable, CursorBuilder.createCursor(table));
    }

    public static void assertCursor(List<? extends Map<String, Object>> expectedTable, Cursor cursor) {
        List<Map<String, Object>> foundTable = new ArrayList<>();
        for (Map<String, Object> row : cursor) {
            foundTable.add(row);
        }
        assertEquals(expectedTable.size(), foundTable.size());
        for (int i = 0; i < expectedTable.size(); i++) {
            assertEquals(expectedTable.get(i), foundTable.get(i));
        }
    }

    public static RowImpl createExpectedRow(Object... rowElements) {
        RowImpl row = new RowImpl((RowIdImpl) null);
        for (int i = 0; i < rowElements.length; i += 2) {
            row.put((String) rowElements[i], rowElements[i + 1]);
        }
        return row;
    }

    public static List<Row> createExpectedTable(Row... rows) {
        return List.of(rows);
    }

    public static void dumpDatabase(Database mdb) throws IOException {
        dumpDatabase(mdb, false);
    }

    public static void dumpDatabase(Database mdb, boolean systemTables) throws IOException {
        dumpDatabase(mdb, systemTables, new PrintWriter(System.out, true));
    }

    public static void dumpTable(Table table) throws IOException {
        dumpTable(table, new PrintWriter(System.out, true));
    }

    public static void dumpProperties(Table table) throws IOException {
        getLogger().log(Level.DEBUG, "TABLE_PROPS: {0}: {1}", table.getName(), table.getProperties());
        for (Column c : table.getColumns()) {
            getLogger().log(Level.DEBUG, "COL_PROPS: {0}: {1}", c.getName(), c.getProperties());
        }
    }

    static void dumpDatabase(Database mdb, boolean systemTables, PrintWriter writer) throws IOException {
        writer.println("DATABASE:");
        for (Table table : mdb) {
            dumpTable(table, writer);
        }
        if (systemTables) {
            for (String sysTableName : mdb.getSystemTableNames()) {
                dumpTable(mdb.getSystemTable(sysTableName), writer);
            }
        }
    }

    static void dumpTable(Table table, PrintWriter writer) throws IOException {
        // make sure all indexes are read
        for (Index index : table.getIndexes()) {
            ((IndexImpl) index).initialize();
        }

        writer.println("TABLE: " + table.getName());
        List<String> colNames = new ArrayList<>();
        for (Column col : table.getColumns()) {
            colNames.add(col.getName());
        }
        writer.println("COLUMNS: " + colNames);
        for (Map<String, Object> row : CursorBuilder.createCursor(table)) {
            writer.println(massageRow(row));
        }
    }

    private static Map<String, Object> massageRow(Map<String, Object> row) throws IOException {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof byte[]) {
                // make byte[] printable
                byte[] bv = (byte[]) v;
                entry.setValue(ByteUtil.toHexString(ByteBuffer.wrap(bv), bv.length));
            } else if (v instanceof ComplexValueForeignKey) {
                // deref complex values
                String str = "ComplexValue(" + v + ")" + ((ComplexValueForeignKey) v).getValues();
                entry.setValue(str);
            }
        }

        return row;
    }

    static void dumpIndex(Index index) throws IOException {
        dumpIndex(index, new PrintWriter(System.out, true));
    }

    static void dumpIndex(Index index, PrintWriter writer) throws IOException {
        writer.println("INDEX: " + index);
        IndexData.EntryCursor ec = ((IndexImpl) index).cursor();
        IndexData.Entry lastE = ec.getLastEntry();
        IndexData.Entry e = null;
        while ((e = ec.getNextEntry()) != lastE) {
            writer.println(e);
        }
    }

    public static void assertSameDate(Date expected, Date found) {
        if (expected == found) {
            return;
        } else if (expected == null || found == null) {
            fail("Expected " + expected + ", found " + found);
        }
        long expTime = expected.getTime();
        long foundTime = found.getTime();
        // there are some rounding issues due to dates being stored as doubles,
        // but it results in a 1 millisecond difference, so i'm not going to worry
        // about it
        assertFalse(expTime != foundTime && Math.abs(expTime - foundTime) > 1,
            "Expected " + expTime + " (" + expected + "), found " + foundTime + " (" + found + ")");
    }

    public static void assertSameDate(Date expected, LocalDateTime found) {
        if (expected == null && found == null) {
            return;
        }
        assertFalse(expected == null || found == null, "Expected " + expected + ", found " + found);

        LocalDateTime expectedLdt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(expected.getTime()),
            ZoneId.systemDefault());

        assertEquals(expectedLdt, found);
    }

    public static void copyFile(File srcFile, File dstFile) throws IOException {
        // FIXME should really be using commons io FileUtils here, but don't want
        // to add dep for one simple test method
        try (OutputStream ostream = new FileOutputStream(dstFile)) {
            InputStream istream = new FileInputStream(srcFile);
            copyStream(istream, ostream);
        }
    }

    public static void copyStream(InputStream istream, OutputStream ostream) throws IOException {
        // FIXME should really be using commons io FileUtils here, but don't want
        // to add dep for one simple test method
        byte[] buf = new byte[1024];
        int numBytes = 0;
        while ((numBytes = istream.read(buf)) >= 0) {
            ostream.write(buf, 0, numBytes);
        }
    }

    public static byte[] toByteArray(File file) throws IOException {
        return toByteArray(new FileInputStream(file), file.length());
    }

    public static byte[] toByteArray(InputStream in, long length) throws IOException {
        try (in) {
            DataInputStream din = new DataInputStream(in);
            byte[] bytes = new byte[(int) length];
            din.readFully(bytes);
            return bytes;
        }
    }

    public static void checkTestDBTable1RowABCDEFG(TestDb testDB, Table table, Row row) {
        assertEquals("abcdefg", row.get("A"), "testDB: " + testDB + "; table: " + table);
        assertEquals("hijklmnop", row.get("B"));
        assertEquals((byte) 2, row.get("C"));
        assertEquals((short) 222, row.get("D"));
        assertEquals(333333333, row.get("E"));
        assertEquals(444.555d, row.get("F"));
        Calendar cal = Calendar.getInstance();
        cal.setTime(row.getDate("G"));
        assertEquals(Calendar.SEPTEMBER, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(1974, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
        assertEquals(Boolean.TRUE, row.get("I"));
    }

    public static void checkTestDBTable1RowA(TestDb testDB, Table table, Row row) {
        assertEquals("a", row.get("A"), "testDB: " + testDB + "; table: " + table);
        assertEquals("b", row.get("B"));
        assertEquals((byte) 0, row.get("C"));
        assertEquals((short) 0, row.get("D"));
        assertEquals(0, row.get("E"));
        assertEquals(0d, row.get("F"));
        Calendar cal = Calendar.getInstance();
        cal.setTime(row.getDate("G"));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(1981, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
        assertEquals(Boolean.FALSE, row.get("I"));
    }

    /**
     * Determines the current logged on user.
     *
     * @return logged on user
     */
    static String getCurrentUser() {
        return Stream.of("user.name", "USER", "USERNAME")
            .map(System::getProperty)
            .filter(s -> !s.isBlank())
            .findFirst().orElse(null);
    }

    public static final File getTestTempDir() {
        return TEST_TEMP_DIR;
    }

    /**
     * Creates a subdirectory of the system's temp file directory.
     * @param _subdirs subdirectory names
     * @return temp directory
     * @throws UncheckedIOException If the subdirectory could not be created
     */
    public static File createTempDir(String... _subdirs) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        for (String dir : _subdirs) {
            tempDir = new File(tempDir, dir);
        }
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new UncheckedIOException(new IOException("Could not create directory " + tempDir));
        }
        return tempDir;
    }

    /**
     * Creates a unique temporary file name using the given prefix and suffix and creates the file.
     *
     * @param _prefix file name prefix
     * @param _suffix file name suffix
     * @param _keep keep temporary file at jvm exit
     * @return temporary file object
     */
    public static File createTempFile(String _prefix, String _suffix, boolean _keep) {
        File tempFile = TestUtil.createTempFileName(_prefix, _suffix);
        try {
            tempFile.createNewFile();
        } catch (IOException _ex) {
            throw new UncheckedIOException(_ex);
        }
        getLogger().log(Level.INFO, "Created temp file {0}", tempFile);
        if (!_keep) {
            tempFile.deleteOnExit();
        }
        return tempFile;
    }

    /**
     * Creates a unique temporary file name using the given prefix and suffix, but does not create the file.
     *
     * @param _prefix file name prefix
     * @param _suffix file name suffix
     * @return temporary file object
     */
    static File createTempFileName(String _prefix, String _suffix) {
        String name = Optional.ofNullable(_prefix).map(p -> p.replace(File.separatorChar, '_')).orElse("");
        if (!name.isBlank() && !name.endsWith("-")) {
            name += "-";
        }
        String suffix = _suffix;
        if (suffix == null || suffix.isBlank()) {
            int idxLastDot = _prefix.lastIndexOf('.');
            if (idxLastDot > -1) {
                suffix = _prefix.substring(idxLastDot);
            }
            if (suffix.isEmpty() || suffix.length() > 6) {
                suffix = ".tmp";
            }
        }
        name += new TempFileNameString() + suffix;
        return new File(getTestTempDir(), name);
    }

    /**
     * Unique string based on current date/time to be used in names of temporary files.
     */
    private static final class TempFileNameString {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private static final AtomicInteger     COUNTER   = new AtomicInteger(1);
        private final String                   name;

        private TempFileNameString() {
            name = LocalDateTime.now().format(FORMATTER) + '_' + String.format("%03d", COUNTER.getAndIncrement());
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
