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

package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.Types;
import java.util.*;

/**
 * @author James Ahlborn
 */
class LongValueTest extends AbstractBaseTest {

    @Test
    void testReadLongValue() throws Exception {

        for (TestDB testDB : TestDB.getSupportedTestDbsReadOnly(Basename.TEST2)) {
            try (Database db = testDB.openMem()) {
                Table table = db.getTable("MSP_PROJECTS");
                Row row = table.getNextRow();
                assertEquals("Jon Iles this is a a vawesrasoih aksdkl fas dlkjflkasjd flkjaslkdjflkajlksj dfl lkasjdf lkjaskldfj "
                    + "lkas dlk lkjsjdfkl; aslkdf lkasjkldjf lka skldf lka sdkjfl;kasjd falksjdfljaslkdjf laskjdfk jalskjd "
                    + "flkj aslkdjflkjkjasljdflkjas jf;lkasjd fjkas dasdf asd fasdf asdf asdmhf lksaiyudfoi jasodfj902384jsdf9 "
                    + "aw90se fisajldkfj lkasj dlkfslkd jflksjadf as",
                    row.get("PROJ_PROP_AUTHOR"));
                assertEquals("T", row.get("PROJ_PROP_COMPANY"));
                assertEquals("Standard", row.get("PROJ_INFO_CAL_NAME"));
                assertEquals("Project1", row.get("PROJ_PROP_TITLE"));
                byte[] foundBinaryData = row.getBytes("RESERVED_BINARY_DATA");
                byte[] expectedBinaryData =
                    toByteArray(new File(DIR_TEST_DATA, "test2BinData.dat"));
                assertArrayEquals(expectedBinaryData, foundBinaryData);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDB#getSupportedFileformats()")
    void testWriteLongValue(FileFormat fileFormat) throws Exception {

        try (Database db = createMem(fileFormat)) {
            Table table =
                new TableBuilder("test")
                    .addColumn(new ColumnBuilder("A", DataType.TEXT))
                    .addColumn(new ColumnBuilder("B", DataType.MEMO))
                    .addColumn(new ColumnBuilder("C", DataType.OLE))
                    .toTable(db);

            String testStr = "This is a test";
            String longMemo = createString(2030);
            byte[] oleValue = toByteArray(new File(DIR_TEST_DATA, "test2BinData.dat"));

            table.addRow(testStr, testStr, null);
            table.addRow(testStr, longMemo, oleValue);
            table.addRow("", "", new byte[0]);
            table.addRow(null, null, null);

            table.reset();

            Row row = table.getNextRow();

            assertEquals(testStr, row.get("A"));
            assertEquals(testStr, row.get("B"));
            assertNull(row.get("C"));

            row = table.getNextRow();

            assertEquals(testStr, row.get("A"));
            assertEquals(longMemo, row.get("B"));
            assertArrayEquals(oleValue, row.getBytes("C"));

            row = table.getNextRow();

            assertEquals("", row.get("A"));
            assertEquals("", row.get("B"));
            assertArrayEquals(new byte[0], row.getBytes("C"));

            row = table.getNextRow();

            assertNull(row.get("A"));
            assertNull(row.get("B"));
            assertNull(row.getBytes("C"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDB#getSupportedFileformats()")
    void testManyMemos(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            final int numColumns = 126;
            TableBuilder bigTableBuilder = new TableBuilder("test");

            for (int i = 0; i < numColumns; i++) {
                bigTableBuilder.addColumn(new ColumnBuilder("column_" + i, DataType.MEMO));
            }

            Table bigTable = bigTableBuilder.toTable(db);

            List<Object[]> expectedRows = new ArrayList<>();

            for (int j = 0; j < 3; j++) {
                Object[] rowData = new String[numColumns];
                for (int i = 0; i < numColumns; i++) {
                    rowData[i] = "v_" + i + ";" + (j + 999);
                }
                expectedRows.add(rowData);
                bigTable.addRow(rowData);
            }

            String extra1 = createString(100);
            String extra2 = createString(2050);

            for (int j = 0; j < 1; j++) {
                Object[] rowData = new String[numColumns];
                for (int i = 0; i < numColumns; i++) {
                    rowData[i] = "v_" + i + ";" + (j + 999) + extra2;
                }
                expectedRows.add(rowData);
                bigTable.addRow(rowData);
            }

            for (int j = 0; j < 2; j++) {
                Object[] rowData = new String[numColumns];
                for (int i = 0; i < numColumns; i++) {
                    String tmp = "v_" + i + ";" + (j + 999);
                    if (i % 3 == 0) {
                        tmp += extra1;
                    } else if (i % 7 == 0) {
                        tmp += extra2;
                    }
                    rowData[i] = tmp;
                }
                expectedRows.add(rowData);
                bigTable.addRow(rowData);
            }

            bigTable.reset();
            Iterator<Object[]> expIter = expectedRows.iterator();
            for (Map<?, ?> row : bigTable) {
                Object[] expectedRow = expIter.next();
                assertEquals(Arrays.asList(expectedRow), new ArrayList<>(row.values()));
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDB#getSupportedFileformats()")
    void testLongValueAsMiddleColumn(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            Table newTable = new TableBuilder("NewTable")
                .addColumn(new ColumnBuilder("a").withSqlType(Types.INTEGER))
                .addColumn(new ColumnBuilder("b").withSqlType(Types.LONGVARCHAR))
                .addColumn(new ColumnBuilder("c").withSqlType(Types.VARCHAR))
                .toTable(db);

            String lval = createString(2000); // "--2000 chars long text--";
            String tval = createString(40); // "--40chars long text--";
            newTable.addRow(1, lval, tval);

            newTable = db.getTable("NewTable");
            Map<String, Object> readRow = newTable.getNextRow();
            assertEquals(1, readRow.get("a"));
            assertEquals(lval, readRow.get("b"));
            assertEquals(tval, readRow.get("c"));
        }
    }

    @Test
    void testUnicodeCompression() throws Exception {
        File dbFile = new File(DIR_TEST_DATA, "V2003/testUnicodeCompV2003.mdb");
        try (Database db = open(FileFormat.V2003, dbFile, true)) {
            StringBuilder sb = new StringBuilder(127);
            for (int i = 1; i <= 0xFF; i++) {
                sb.append((char) i);
            }
            String longStr = sb.toString();

            String[] expectedStrs = {
                "only ascii chars",
                "\u00E4\u00E4kk\u00F6si\u00E4",
                "\u041C\u0438\u0440",
                "\u03F0\u03B1\u1F76 \u03C4\u1F79\u03C4' \u1F10\u03B3\u1F7C \u039A\u1F7B\u03F0\u03BB\u03C9\u03C0\u03B1",
                "\u6F22\u5B57\u4EEE\u540D\u4EA4\u3058\u308A\u6587",
                "3L9\u001D52\u0002_AB(\u00A5\u0005!!V",
                "\u00FCmlaut",
                longStr};

            Table t = db.getTable("Table");
            for (Row row : t) {
                int id = (Integer) row.get("ID");
                String str = (String) row.get("Unicode");
                assertEquals(expectedStrs[id - 1], str);
            }

            ColumnImpl col = (ColumnImpl) t.getColumn("Unicode");

            ByteBuffer bb = col.write(longStr, 1000);

            assertEquals(longStr.length() + 2, bb.remaining());

            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            assertEquals(longStr, col.read(bytes));

            longStr = longStr.replace('a', '\u0440');

            bb = col.write(longStr, 1000);

            assertEquals(longStr.length() * 2, bb.remaining());

            bytes = new byte[bb.remaining()];
            bb.get(bytes);
            assertEquals(longStr, col.read(bytes));
        }
    }
}
