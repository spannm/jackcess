/*
Copyright (c) 2008 Health Market Science, Inc.

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

import static io.github.spannm.jackcess.test.Basename.EMOTICONS;
import static io.github.spannm.jackcess.test.Basename.INDEX_CODES;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("checkstyle:MethodNameCheck")
public class IndexCodesTest extends AbstractBaseTest {

    private static final Map<Character, String> SPECIAL_CHARS = new HashMap<>();
    static {
        SPECIAL_CHARS.put('\b', "\\b");
        SPECIAL_CHARS.put('\t', "\\t");
        SPECIAL_CHARS.put('\n', "\\n");
        SPECIAL_CHARS.put('\f', "\\f");
        SPECIAL_CHARS.put('\r', "\\r");
        SPECIAL_CHARS.put('\"', "\\\"");
        SPECIAL_CHARS.put('\'', "\\'");
        SPECIAL_CHARS.put('\\', "\\\\");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource({INDEX_CODES, EMOTICONS})
    void indexCodes(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            db.setDateTimeType(DateTimeType.DATE);

            for (Table t : db) {
                for (Index index : t.getIndexes()) {
                    // getLogger().log(Level.FINE, "Checking {0}.{1}", new Object[] {t.getName(), index.getName()});
                    checkIndexEntries(testDb, t, index);
                }
            }
        }
    }

    /**
     * Tests the "international ext" characters, whose codes end with a suffix whose length grows with the number of characters which took that path. The
     * expected keys were read back out of the index pages Access built for U+3041 (crazy flag set) and U+3042 (crazy flag clear).
     */
    @Test
    void internationalExtCodes() throws Exception {
        try (Database db = createDbMem(FileFormat.V2010, true)) {
            IndexData.ColumnDescriptor col = getTextIndexColumn(db);

            // one suffix repeat covers up to 7 chars, so these are the two boundaries either side of the first extra repeat
            assertIndexKey("7f7f02010101a0ff0280ff8000", col, repeatChar('ぁ', 1));
            assertIndexKey("7f7f02010101ff0280ff8000", col, repeatChar('あ', 1));
            assertIndexKey("7f7f027f027f027f027f027f027f027f02010101aaaaa8ff028080ff808000", col, repeatChar('ぁ', 8));
            assertIndexKey("7f7f027f027f027f027f027f027f027f02010101ff028080ff808000", col, repeatChar('あ', 8));
            assertIndexKey("7f7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "010101aaaaaaaaaaa0ff02808080ff80808000", col, repeatChar('ぁ', 16));
        }
    }

    /**
     * Tests that a key longer than the maximum is truncated the way Access truncates it, with a digest of the discarded bytes. The expected key was read
     * back out of the index page Access built.
     */
    @Test
    void truncatedKey() throws Exception {
        try (Database db = createDbMem(FileFormat.V2010, true)) {
            IndexData.ColumnDescriptor col = getTextIndexColumn(db);

            // 200 chars of U+3041 encode to more than the maximum key length, so Access keeps the leading 508 bytes and ends the key with a digest of
            // everything it discarded
            byte[] fullKey = encodeIndexKey(col, repeatChar('ぁ', 200));
            assertThat(fullKey).hasSize(533);

            // the truncated form is what Access actually stores
            String expected = "7f7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f02"
                + "7f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f027f"
                + "027f027f02010101aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaa8ff0280808080808080808080808080808080808080808080"
                + "80808080808080ff8080808080d06b";
            assertThat(toCompactHex(IndexData.truncateEntryBytes(fullKey))).isEqualTo(expected);
        }
    }

    private static IndexData.ColumnDescriptor getTextIndexColumn(Database db) throws Exception {
        Table t = new TableBuilder("test").addColumn(new ColumnBuilder("data", DataType.MEMO)).addIndex(new IndexBuilder("dataidx").withColumns("data")).toTable(db);
        IndexImpl idx = (IndexImpl) t.getIndex("dataidx");
        return idx.getIndexData().getColumns().get(0);
    }

    private static void assertIndexKey(String expected, IndexData.ColumnDescriptor col, String value) throws Exception {
        assertThat(toCompactHex(encodeIndexKey(col, value))).as("key for %s", toUnicodeStr(value)).isEqualTo(expected);
    }

    private static byte[] encodeIndexKey(IndexData.ColumnDescriptor col, String value) throws Exception {
        ByteUtil.ByteStream bout = new ByteUtil.ByteStream();
        col.writeValue(value, bout);
        return bout.toByteArray();
    }

    private static String repeatChar(char c, int num) {
        StringBuilder sb = new StringBuilder(num);
        for (int i = 0; i < num; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String toCompactHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static void checkIndexEntries(TestDb testDB, Table t, Index index) throws Exception {
        // index.initialize();
        // getStaticLogger().log(Level.FINE, "Ind {0}", index);

        Cursor cursor = CursorBuilder.createCursor(index);
        while (cursor.moveToNextRow()) {

            Row row = cursor.getCurrentRow();

            Object data = row.get("data");
            if (testDB.getExpectedFileFormat() == FileFormat.V1997 && data instanceof String && ((String) data).contains("\uFFFD")) {
                // this row has a character not supported in the v1997 charset
                continue;
            }

            Cursor.Position curPos = cursor.getSavepoint().getCurrentPosition();
            boolean success = false;
            try {
                findRow(testDB, t, index, row, curPos);
                success = true;
            } finally {
                if (!success) {
                    getStaticLogger().log(Level.FINE, "CurPos: {0}", curPos);
                    getStaticLogger().log(Level.FINE, "Value: {0}: {1}", new Object[] {row, toUnicodeStr(row.get("data"))});
                }
            }
        }

    }

    private static void findRow(final TestDb testDB, Table t, Index index, Row expectedRow, Cursor.Position expectedPos) throws Exception {
        Object[] idxRow = ((IndexImpl) index).constructIndexRow(expectedRow);
        Cursor cursor = CursorBuilder.createCursor(index, idxRow, idxRow);

        Cursor.Position startPos = cursor.getSavepoint().getCurrentPosition();

        cursor.beforeFirst();
        while (cursor.moveToNextRow()) {
            Row row = cursor.getCurrentRow();
            if (expectedRow.equals(row)) {
                // verify that the entries are indeed equal
                Cursor.Position curPos = cursor.getSavepoint().getCurrentPosition();
                assertThat(entryToString(curPos)).isEqualTo(entryToString(expectedPos));
                return;
            }
        }

        Assertions.fail("testDB: " + testDB + ": Could not find expected row " + expectedRow + " starting at " + entryToString(startPos));
    }

    //////
    //
    // The code below is for use in reverse engineering index entries.
    //
    //////

    @Test @Disabled
    void createIsoFile() throws Exception {
        try (Database db = createDbMem(FileFormat.V2000, true)) {
            Table t = new TableBuilder("test").addColumn(new ColumnBuilder("row", DataType.TEXT)).addColumn(new ColumnBuilder("data", DataType.TEXT)).toTable(db);

            for (int i = 0; i < 256; i++) {
                String str = "AA" + (char) i + "AA";
                t.addRow("row" + i, str);
            }
        }
    }

    @Test @Disabled
    void createAltIsoFile() throws Exception {
        try (Database db = TestUtil.openCopy(FileFormat.V2000, new File("/tmp/test_ind.mdb"), true)) {
            Table t = db.getTable("Table1");

            for (int i = 0; i < 256; i++) {
                String str = "AA" + (char) i + "AA";
                t.addRow("row" + i, str, (byte) 42 + i, (short) 53 + i, 13 * i, 6.7d / i, null, null, true);
            }
        }
    }

    @SuppressWarnings("unused")
    @Test @Disabled
    void writeAllCodesMdb() throws Exception {
        try (Database db = createDbMem(FileFormat.V2000, true)) {
            Table t = new TableBuilder("Table5").addColumn(new ColumnBuilder("name", DataType.TEXT)).addColumn(new ColumnBuilder("data", DataType.TEXT)).toTable(db);

            char c = (char) 0x3041; // crazy 7F 02 ... A0
            char c2 = (char) 0x30A2; // crazy 7F 02 ...
            char c3 = (char) 0x2045; // inat 27 ... 1C
            char c4 = (char) 0x3043; // crazy 7F 03 ... A0
            char c5 = (char) 0x3046; // crazy 7F 04 ...
            char c6 = (char) 0x30F6; // crazy 7F 0D ... A0
            char c7 = (char) 0x3099; // unprint 03
            char c8 = (char) 0x0041; // A
            char c9 = (char) 0x002D; // - (unprint)
            char c10 = (char) 0x20E1; // unprint F2
            char c11 = (char) 0x309A; // unprint 04
            char c12 = (char) 0x01C4; // (long extra)
            char c13 = (char) 0x005F; // _ (long inline)
            char c14 = (char) 0xFFFE; // removed

            char[] cs = {
                c7,
                c8,
                c3,
                c12,
                c13,
                c14,
                c,
                c2,
                c9};
            addCombos(t, 0, "", cs, 5);

            // t = new TableBuilder("Table2")
            // .addColumn(new ColumnBuilder("data", DataType.TEXT))
            // .toTable(db);

            // writeChars(0x0000, t);

            // t = new TableBuilder("Table3")
            // .addColumn(new ColumnBuilder("data", DataType.TEXT))
            // .toTable(db);

            // writeChars(0x0400, t);
        }

        // Table t = new TableBuilder("Table1")
        // .addColumn(new ColumnBuilder("key", DataType.TEXT))
        // .addColumn(new ColumnBuilder("data", DataType.TEXT))
        // .toTable(db);

        // for(int i = 0; i <= 0xFFFF; i++) {
        // // skip non-char chars
        // char c = (char)i;
        // if(Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
        // continue;
        // }
        // String key = toUnicodeStr(c);
        // String str = "AA" + c + "AA";
        // t.addRow(key, str);
        // }

    }

    @Test
    @Disabled
    void readAllCodesMdb() throws Exception {
        try (Database db = TestUtil.openCopy(FileFormat.V2000, new File("/data2/jackcess_test/testStillMoreCodes.mdb"))) {
            Table t = db.getTable("Table5");

            Index ind = t.getIndexes().iterator().next();
            ((IndexImpl) ind).initialize();

            getLogger().log(Level.FINE, "Ind {0}", ind);

            Cursor cursor = CursorBuilder.createCursor(ind);
            while (cursor.moveToNextRow()) {
                String entryStr = entryToString(cursor.getSavepoint().getCurrentPosition());
                getLogger().log(Level.FINE, "=======");
                getLogger().log(Level.FINE, "Entry Bytes: {0}", entryStr);
                getLogger().log(Level.FINE, "Value: {0}; {1}", new Object[] {cursor.getCurrentRow(), toUnicodeStr(cursor.getCurrentRow().get("data"))});
            }
        }
    }

    private int addCombos(Table t, int rowNum, String s, char[] cs, int len) throws IOException {
        if (s.length() >= len) {
            return rowNum;
        }

        for (char element : cs) {
            String name = "row" + rowNum++;
            String ss = s + element;
            t.addRow(name, ss);
            rowNum = addCombos(t, rowNum, ss, cs, len);
        }

        return rowNum;
    }

    void writeChars(int hibyte, Table t) throws IOException {
        char other = (char) (hibyte | 0x41);
        for (int i = 0; i < 0xFF; i++) {
            char c = (char) (hibyte | i);
            String str = "" + other + c + other;
            t.addRow(str);
        }
    }

    @Test
    @Disabled
    void readIsoMdb() throws Exception {
        try (Database db = TestUtil.openDb(FileFormat.V2000, new File("/tmp/test_ind3.mdb"))) {
            Table t = db.getTable("Table1");
            Index index = t.getIndex("B");
            ((IndexImpl) index).initialize();

            getLogger().log(Level.FINE, "Ind {0}", index);

            Cursor cursor = CursorBuilder.createCursor(index);
            while (cursor.moveToNextRow()) {
                getLogger().log(Level.FINE, "=======");
                getLogger().log(Level.FINE, "Savepoint: {0}", cursor.getSavepoint());
                getLogger().log(Level.FINE, "Value: {0}", cursor.getCurrentRow());
            }
        }
    }

    @Test @Disabled
    void reverseIsoMdb2010() throws Exception {
        try (Database db = TestUtil.openDb(FileFormat.V2010, new File("/data2/jackcess_test/testAllIndexCodes3_2010.accdb"))) {
            Table t = db.getTable("Table1");
            Index index = t.getIndexes().iterator().next();
            ((IndexImpl) index).initialize();
            getLogger().log(Level.FINE, "Index {0}", index);

            Pattern inlinePat = Pattern.compile("7F 0E 02 0E 02 (.*)0E 02 0E 02 01 00");
            Pattern unprintPat = Pattern.compile("01 01 01 80 (.+) 06 (.+) 00");
            Pattern unprint2Pat = Pattern.compile("0E 02 0E 02 0E 02 0E 02 01 02 (.+) 00");
            Pattern inatPat = Pattern.compile("7F 0E 02 0E 02 (.*)0E 02 0E 02 01 02 02 (.+) 00");
            Pattern inat2Pat = Pattern.compile("7F 0E 02 0E 02 (.*)0E 02 0E 02 01 (02 02 (.+))?01 01 (.*)FF 02 80 FF 80 00");

            Map<Character, String[]> inlineCodes = new TreeMap<>();
            Map<Character, String[]> unprintCodes = new TreeMap<>();
            Map<Character, String[]> unprint2Codes = new TreeMap<>();
            Map<Character, String[]> inatInlineCodes = new TreeMap<>();
            Map<Character, String[]> inatExtraCodes = new TreeMap<>();
            Map<Character, String[]> inat2Codes = new TreeMap<>();
            Map<Character, String[]> inat2ExtraCodes = new TreeMap<>();
            Map<Character, String[]> inat2CrazyCodes = new TreeMap<>();

            Cursor cursor = CursorBuilder.createCursor(index);
            while (cursor.moveToNextRow()) {
                // getLogger().log(Level.FINE, "=======");
                // getLogger().log(Level.FINE, "Savepoint: {0}", cursor.getSavepoint());
                // getLogger().log(Level.FINE, "Value: {0}", cursor.getCurrentRow());
                Cursor.Savepoint savepoint = cursor.getSavepoint();
                String entryStr = entryToString(savepoint.getCurrentPosition());

                Row row = cursor.getCurrentRow();
                String value = row.getString("data");
                String key = row.getString("key");
                char c = value.charAt(2);

                getLogger().log(Level.FINE, "=======");
                getLogger().log(Level.FINE, "RowId: {0}", savepoint.getCurrentPosition().getRowId());
                getLogger().log(Level.FINE, "Entry: {0}", entryStr);
                getLogger().log(Level.FINE, "Value: ({0}) {1}", new Object[] {key, value});
                getLogger().log(Level.FINE, "Char: {0}, {1}, {2}", new Object[] {c, (int) c, toUnicodeStr(c)});

                String type = null;
                if (entryStr.endsWith("01 00")) {

                    // handle inline codes
                    type = "INLINE";
                    Matcher m = inlinePat.matcher(entryStr);
                    m.find();
                    handleInlineEntry(m.group(1), c, inlineCodes);

                } else if (entryStr.contains("01 01 01 80")) {

                    // handle most unprintable codes
                    type = "UNPRINTABLE";
                    Matcher m = unprintPat.matcher(entryStr);
                    m.find();
                    handleUnprintableEntry(m.group(2), c, unprintCodes);

                } else if (entryStr.contains("01 02 02") && !entryStr.contains("FF 02 80 FF 80")) {

                    // handle chars w/ symbols
                    type = "CHAR_WITH_SYMBOL";
                    Matcher m = inatPat.matcher(entryStr);
                    m.find();
                    handleInternationalEntry(m.group(1), m.group(2), c, inatInlineCodes, inatExtraCodes);

                } else if (entryStr.contains("0E 02 0E 02 0E 02 0E 02 01 02")) {

                    // handle chars w/ symbols
                    type = "UNPRINTABLE_2";
                    Matcher m = unprint2Pat.matcher(entryStr);
                    m.find();
                    handleUnprintable2Entry(m.group(1), c, unprint2Codes);

                } else if (entryStr.contains("FF 02 80 FF 80")) {

                    type = "CRAZY_INAT";
                    Matcher m = inat2Pat.matcher(entryStr);
                    m.find();
                    handleInternational2Entry(m.group(1), m.group(3), m.group(4), c, inat2Codes, inat2ExtraCodes, inat2CrazyCodes);

                } else {

                    // throw new JackcessRuntimeException("Unhandled " + entryStr);
                    getLogger().log(Level.WARNING, "unhandled {0}", entryStr);
                }

                getLogger().log(Level.FINE, "Type {0}", type);
            }

            getLogger().log(Level.FINE, "\n*** CODES");
            for (int i = 0; i <= 0xFFFF; i++) {

                if (i == 256) {
                    getLogger().log(Level.FINE, "\n*** EXTENDED CODES");
                }

                // skip non-char chars
                char c = (char) i;
                if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                    continue;
                }

                if (c == (char) 0xFFFE) {
                    // this gets replaced with FFFD, treat it the same
                    c = (char) 0xFFFD;
                }

                Character cc = c;
                String[] chars = inlineCodes.get(cc);
                if (chars != null) {
                    if (chars.length == 1 && chars[0].isEmpty()) {
                        getLogger().log(Level.FINE, "X");
                    } else {
                        getLogger().log(Level.FINE, "S{0}", toByteString(chars));
                    }
                    continue;
                }

                chars = inatInlineCodes.get(cc);
                if (chars != null) {
                    String[] extra = inatExtraCodes.get(cc);
                    getLogger().log(Level.FINE, "I{0}, {1}", new Object[] {toByteString(chars), toByteString(extra)});
                    continue;
                }

                chars = unprintCodes.get(cc);
                if (chars != null) {
                    getLogger().log(Level.FINE, "U{0}", toByteString(chars));
                    continue;
                }

                chars = unprint2Codes.get(cc);
                if (chars != null) {
                    if (chars.length > 1) {
                        throw new JackcessRuntimeException("Long unprint codes");
                    }
                    int val = Integer.parseInt(chars[0], 16) - 2;
                    String valStr = ByteUtil.toHexString(new byte[] {
                        (byte) val}).trim();
                    getLogger().log(Level.FINE, "P{0}", valStr);
                    continue;
                }

                chars = inat2Codes.get(cc);
                if (chars != null) {
                    String[] crazyCodes = inat2CrazyCodes.get(cc);
                    String crazyCode = "";
                    if (crazyCodes != null) {
                        if (crazyCodes.length != 1 || !"A0".equals(crazyCodes[0])) {
                            throw new JackcessRuntimeException("CC " + Arrays.toString(crazyCodes));
                        }
                        crazyCode = "1";
                    }

                    String[] extra = inat2ExtraCodes.get(cc);
                    getLogger().log(Level.FINE, "Z{0}, {1}, {2}", new Object[] {toByteString(chars), toByteString(extra), crazyCode});
                    continue;
                }

                throw new JackcessRuntimeException("Unhandled char " + toUnicodeStr(c));
            }
            getLogger().log(Level.FINE, "\n*** END CODES");
        }
    }

    @Test @Disabled
    void reverseIsoMdb() throws Exception {
        try (Database db = TestUtil.openDb(FileFormat.V2000, new File("/data2/jackcess_test/testAllIndexCodes3.mdb"))) {
            Table t = db.getTable("Table1");
            Index index = t.getIndexes().iterator().next();
            ((IndexImpl) index).initialize();
            getLogger().log(Level.FINE, "Index {0}", index);

            Pattern inlinePat = Pattern.compile("7F 4A 4A (.*)4A 4A 01 00");
            Pattern unprintPat = Pattern.compile("01 01 01 80 (.+) 06 (.+) 00");
            Pattern unprint2Pat = Pattern.compile("4A 4A 4A 4A 01 02 (.+) 00");
            Pattern inatPat = Pattern.compile("7F 4A 4A (.*)4A 4A 01 02 02 (.+) 00");
            Pattern inat2Pat = Pattern.compile("7F 4A 4A (.*)4A 4A 01 (02 02 (.+))?01 01 (.*)FF 02 80 FF 80 00");

            Map<Character, String[]> inlineCodes = new TreeMap<>();
            Map<Character, String[]> unprintCodes = new TreeMap<>();
            Map<Character, String[]> unprint2Codes = new TreeMap<>();
            Map<Character, String[]> inatInlineCodes = new TreeMap<>();
            Map<Character, String[]> inatExtraCodes = new TreeMap<>();
            Map<Character, String[]> inat2Codes = new TreeMap<>();
            Map<Character, String[]> inat2ExtraCodes = new TreeMap<>();
            Map<Character, String[]> inat2CrazyCodes = new TreeMap<>();

            Cursor cursor = CursorBuilder.createCursor(index);
            while (cursor.moveToNextRow()) {
                getLogger().log(Level.FINE, "=======");
                getLogger().log(Level.FINE, "Savepoint: {0}", cursor.getSavepoint());
                getLogger().log(Level.FINE, "Value: {0}", cursor.getCurrentRow());
                Cursor.Savepoint savepoint = cursor.getSavepoint();
                String entryStr = entryToString(savepoint.getCurrentPosition());

                Row row = cursor.getCurrentRow();
                String value = row.getString("data");
                String key = row.getString("key");
                char c = value.charAt(2);
                getLogger().log(Level.FINE, "=======");
                getLogger().log(Level.FINE, "RowId: {0}", savepoint.getCurrentPosition().getRowId());
                getLogger().log(Level.FINE, "Entry: {0}", entryStr);
                // getLogger().log(Level.FINE, "Row: {0}", row);
                getLogger().log(Level.FINE, "Value: ({0}) {1}", new Object[] {key, value});
                getLogger().log(Level.FINE, "Char: {0}, {1}, {2}", new Object[] {c, (int) c, toUnicodeStr(c)});

                String type = null;
                if (entryStr.endsWith("01 00")) {

                    // handle inline codes
                    type = "INLINE";
                    Matcher m = inlinePat.matcher(entryStr);
                    m.find();
                    handleInlineEntry(m.group(1), c, inlineCodes);

                } else if (entryStr.contains("01 01 01 80")) {

                    // handle most unprintable codes
                    type = "UNPRINTABLE";
                    Matcher m = unprintPat.matcher(entryStr);
                    m.find();
                    handleUnprintableEntry(m.group(2), c, unprintCodes);

                } else if (entryStr.contains("01 02 02") && !entryStr.contains("FF 02 80 FF 80")) {

                    // handle chars w/ symbols
                    type = "CHAR_WITH_SYMBOL";
                    Matcher m = inatPat.matcher(entryStr);
                    m.find();
                    handleInternationalEntry(m.group(1), m.group(2), c, inatInlineCodes, inatExtraCodes);

                } else if (entryStr.contains("4A 4A 4A 4A 01 02")) {

                    // handle chars w/ symbols
                    type = "UNPRINTABLE_2";
                    Matcher m = unprint2Pat.matcher(entryStr);
                    m.find();
                    handleUnprintable2Entry(m.group(1), c, unprint2Codes);

                } else if (entryStr.contains("FF 02 80 FF 80")) {

                    type = "CRAZY_INAT";
                    Matcher m = inat2Pat.matcher(entryStr);
                    m.find();
                    handleInternational2Entry(m.group(1), m.group(3), m.group(4), c, inat2Codes, inat2ExtraCodes, inat2CrazyCodes);

                } else {

                    throw new JackcessRuntimeException("Unhandled " + entryStr);
                }

                getLogger().log(Level.FINE, "Type: {0}", type);
            }

            getLogger().log(Level.FINE, "\n*** CODES");
            for (int i = 0; i <= 0xFFFF; i++) {

                if (i == 256) {
                    getLogger().log(Level.FINE, "\n*** EXTENDED CODES");
                }

                // skip non-char chars
                char c = (char) i;
                if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                    continue;
                }

                if (c == (char) 0xFFFE) {
                    // this gets replaced with FFFD, treat it the same
                    c = (char) 0xFFFD;
                }

                Character cc = c;
                String[] chars = inlineCodes.get(cc);
                if (chars != null) {
                    if (chars.length == 1 && chars[0].isEmpty()) {
                        getLogger().log(Level.FINE, "X");
                    } else {
                        getLogger().log(Level.FINE, "S{0}", toByteString(chars));
                    }
                    continue;
                }

                chars = inatInlineCodes.get(cc);
                if (chars != null) {
                    String[] extra = inatExtraCodes.get(cc);
                    getLogger().log(Level.FINE, "I{0}, {1}", new Object[] {toByteString(chars), toByteString(extra)});
                    continue;
                }

                chars = unprintCodes.get(cc);
                if (chars != null) {
                    getLogger().log(Level.FINE, "U{0}", toByteString(chars));
                    continue;
                }

                chars = unprint2Codes.get(cc);
                if (chars != null) {
                    if (chars.length > 1) {
                        throw new JackcessRuntimeException("Long unprint codes");
                    }
                    int val = Integer.parseInt(chars[0], 16) - 2;
                    String valStr = ByteUtil.toHexString(new byte[] {(byte) val}).trim();
                    getLogger().log(Level.FINE, "P{0}", valStr);
                    continue;
                }

                chars = inat2Codes.get(cc);
                if (chars != null) {
                    String[] crazyCodes = inat2CrazyCodes.get(cc);
                    String crazyCode = "";
                    if (crazyCodes != null) {
                        if (crazyCodes.length != 1 || !"A0".equals(crazyCodes[0])) {
                            throw new JackcessRuntimeException("CC " + Arrays.toString(crazyCodes));
                        }
                        crazyCode = "1";
                    }

                    String[] extra = inat2ExtraCodes.get(cc);
                    getLogger().log(Level.FINE, "Z{0}, {1}, {2}", new Object[] {toByteString(chars), toByteString(extra), crazyCode});
                    continue;
                }

                throw new JackcessRuntimeException("Unhandled char " + toUnicodeStr(c));
            }
            getLogger().log(Level.FINE, "\n*** END CODES");
        }
    }

    private static String toByteString(String[] chars) {
        String str = join(chars, "", "");
        if (!str.isEmpty() && str.charAt(0) == '0') {
            str = str.substring(1);
        }
        return str;
    }

    private static void handleInlineEntry(String entryCodes, char c, Map<Character, String[]> inlineCodes) {
        inlineCodes.put(c, entryCodes.trim().split(" "));
    }

    private static void handleUnprintableEntry(String entryCodes, char c, Map<Character, String[]> unprintCodes) {
        unprintCodes.put(c, entryCodes.trim().split(" "));
    }

    private static void handleUnprintable2Entry(String entryCodes, char c, Map<Character, String[]> unprintCodes) {
        unprintCodes.put(c, entryCodes.trim().split(" "));
    }

    private static void handleInternationalEntry(String inlineCodes, String entryCodes, char c, Map<Character, String[]> inatInlineCodes, Map<Character, String[]> inatExtraCodes) {
        inatInlineCodes.put(c, inlineCodes.trim().split(" "));
        inatExtraCodes.put(c, entryCodes.trim().split(" "));
    }

    private static void handleInternational2Entry(String inlineCodes, String entryCodes, String crazyCodes, char c, Map<Character, String[]> inatInlineCodes, Map<Character, String[]> inatExtraCodes,
        Map<Character, String[]> inatCrazyCodes) {
        inatInlineCodes.put(c, inlineCodes.trim().split(" "));
        if (entryCodes != null) {
            inatExtraCodes.put(c, entryCodes.trim().split(" "));
        }
        if (crazyCodes != null && !crazyCodes.isEmpty()) {
            inatCrazyCodes.put(c, crazyCodes.trim().split(" "));
        }
    }

    public static String toUnicodeStr(Object obj) {
        StringBuilder sb = new StringBuilder();
        for (char c : obj.toString().toCharArray()) {
            sb.append(toUnicodeStr(c)).append(' ');
        }
        return sb.toString();
    }

    private static String toUnicodeStr(char c) {
        String specialStr = SPECIAL_CHARS.get(c);
        if (specialStr != null) {
            return specialStr;
        }

        String digits = Integer.toHexString(c).toUpperCase();
        while (digits.length() < 4) {
            digits = "0" + digits;
        }
        return "\\u" + digits;
    }

    private static String join(String[] strs, String joinStr, String prefixStr) {
        if (strs == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].isEmpty()) {
                continue;
            }
            builder.append(prefixStr).append(strs[i]);
            if (i < strs.length - 1) {
                builder.append(joinStr);
            }
        }
        return builder.toString();
    }

    public static String entryToString(Cursor.Position curPos) throws Exception, IllegalAccessException {
        Field eField = curPos.getClass().getDeclaredField("entry");
        eField.setAccessible(true);
        IndexData.Entry entry = (IndexData.Entry) eField.get(curPos);
        Field ebField = entry.getClass().getDeclaredField("entryBytes");
        ebField.setAccessible(true);
        byte[] entryBytes = (byte[]) ebField.get(entry);

        return ByteUtil.toHexString(ByteBuffer.wrap(entryBytes), 0, entryBytes.length, false);
    }

}
