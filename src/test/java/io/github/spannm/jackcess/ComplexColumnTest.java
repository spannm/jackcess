package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.COMPLEX_DATA;
import static io.github.spannm.jackcess.test.Basename.UNSUPPORTED_FIELDS;

import io.github.spannm.jackcess.complex.*;
import io.github.spannm.jackcess.impl.ByteUtil;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.PageChannel;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")
class ComplexColumnTest extends AbstractBaseTest {

    private static final byte[] TEST_ENC_BYTES  =
    new byte[] {b(0x01), b(0x00), b(0x00), b(0x00), b(0x3A), b(0x00), b(0x00), b(0x00), b(0x78), b(0x5E), b(0x13), b(0x61), b(0x60), b(0x60), b(0x60), b(0x04), b(0x62), b(0x16), b(0x20), b(
        0x2E), b(0x61), b(0xA8), b(0x00), b(0x62), b(0x20), b(0x9D), b(0x91), b(0x59), b(0xAC), b(0x00), b(0x44), b(0xC5), b(0xF9), b(0xB9), b(0xA9), b(0x0A), b(0x25), b(0xA9), b(0xC5), b(
            0x25), b(0x0A), b(0x29), b(0x89), b(0x25), b(0x89), b(0x0A), b(
                0x69), b(0xF9), b(0x45), b(0x0A), b(0x89), b(0x25), b(0x25), b(0x89), b(0xC9), b(0x19), b(0xB9), b(0xA9), b(0x79), b(0x25), b(0x7A), b(0x00), b(0x52), b(0xA9), b(0x0F), b(0x7A)
    };

    private static final byte[] TEST_BYTES      = getAsciiBytes("this is some test data for attachment.");

    private static final byte[] TEST2_ENC_BYTES =
    new byte[] {b(0x01), b(0x00), b(0x00), b(0x00), b(0x3F), b(0x00), b(0x00), b(0x00), b(0x78), b(0x5E), b(0x13), b(0x61), b(0x60), b(0x60), b(0x60), b(0x04), b(0x62), b(0x16), b(0x20), b(
        0x2E), b(0x61), b(0xA8), b(0x00), b(0x62), b(0x20), b(0x9D), b(0x91), b(0x59), b(0xAC), b(0x00), b(0x44), b(0xC5), b(0xF9), b(0xB9), b(0xA9), b(0x0A), b(0xB9), b(0xF9), b(0x45), b(
            0xA9), b(0x0A), b(0x25), b(0xA9), b(0xC5), b(0x25), b(0x0A), b(0x29), b(0x89), b(0x25), b(0x89), b(0x0A), b(
                0x69), b(0xF9), b(0x45), b(0x0A), b(0x89), b(0x25), b(0x25), b(0x89), b(0xC9), b(0x19), b(0xB9), b(0xA9), b(0x79), b(0x25), b(0x7A), b(0x00), b(0xA5), b(0x0B), b(0x11), b(0x4D)
    };

    private static final byte[] TEST2_BYTES     = getAsciiBytes("this is some more test data for attachment.");

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void testVersions(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            db.setDateTimeType(DateTimeType.DATE);
            db.setTimeZone(TEST_TZ);

            Table t1 = db.getTable("Table1");
            Column col = t1.getColumn("append-memo-data");
            assertTrue(col.isAppendOnly());
            Column verCol = col.getVersionHistoryColumn();
            assertNotNull(verCol);
            assertEquals(ComplexDataType.VERSION_HISTORY,
                verCol.getComplexInfo().getType());

            for (Row row : t1) {
                String rowId = row.getString("id");
                ComplexValueForeignKey complexValueFk =
                    (ComplexValueForeignKey) verCol.getRowValue(row);

                String curValue = (String) col.getRowValue(row);

                if (rowId.equals("row1")) {
                    checkVersions(1, complexValueFk, curValue);
                } else if (rowId.equals("row2")) {
                    checkVersions(2, complexValueFk, curValue,
                        "row2-memo", new Date(1315876862334L));
                } else if (rowId.equals("row3")) {
                    checkVersions(3, complexValueFk, curValue,
                        "row3-memo-again", new Date(1315876965382L),
                        "row3-memo-revised", new Date(1315876953077L),
                        "row3-memo", new Date(1315876879126L));
                } else if (rowId.equals("row4")) {
                    checkVersions(4, complexValueFk, curValue,
                        "row4-memo", new Date(1315876945758L));
                } else {
                    fail();
                }
            }

            Object[] row8 = {"row8", Column.AUTO_NUMBER, "some-data", "row8-memo", Column.AUTO_NUMBER, Column.AUTO_NUMBER};
            t1.addRow(row8);

            ComplexValueForeignKey row8ValFk = (ComplexValueForeignKey) verCol.getRowValue(row8);
            Date upTime = new Date();
            row8ValFk.addVersion("row8-memo", upTime);
            checkVersions(row8ValFk.get(), row8ValFk, "row8-memo",
                "row8-memo", upTime);

            Cursor cursor = CursorBuilder.createCursor(t1);
            assertTrue(cursor.findFirstRow(t1.getColumn("id"), "row3"));
            ComplexValueForeignKey row3ValFk = (ComplexValueForeignKey) cursor.getCurrentRowValue(verCol);
            cursor.setCurrentRowValue(col, "new-value");
            Version v = row3ValFk.addVersion("new-value", upTime);
            checkVersions(3, row3ValFk, "new-value",
                "new-value", upTime,
                "row3-memo-again", new Date(1315876965382L),
                "row3-memo-revised", new Date(1315876953077L),
                "row3-memo", new Date(1315876879126L));

            assertThrows(UnsupportedOperationException.class, v::update);

            checkVersions(3, row3ValFk, "new-value",
                "new-value", upTime,
                "row3-memo-again", new Date(1315876965382L),
                "row3-memo-revised", new Date(1315876953077L),
                "row3-memo", new Date(1315876879126L));

            assertThrows(UnsupportedOperationException.class, v::delete);

            checkVersions(3, row3ValFk, "new-value",
                "new-value", upTime,
                "row3-memo-again", new Date(1315876965382L),
                "row3-memo-revised", new Date(1315876953077L),
                "row3-memo", new Date(1315876879126L));

            assertThrows(UnsupportedOperationException.class, v.getComplexValueForeignKey()::deleteAllValues);

            checkVersions(3, row3ValFk, "new-value",
                "new-value", upTime,
                "row3-memo-again", new Date(1315876965382L),
                "row3-memo-revised", new Date(1315876953077L),
                "row3-memo", new Date(1315876879126L));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void testAttachments(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Table1");
            Column col = t1.getColumn("attach-data");
            assertEquals(ComplexDataType.ATTACHMENT,
                col.getComplexInfo().getType());

            for (Row row : t1) {
                String rowId = row.getString("id");
                ComplexValueForeignKey complexValueFk =
                    (ComplexValueForeignKey) col.getRowValue(row);

                if (rowId.equals("row1")) {
                    checkAttachments(1, complexValueFk);
                } else if (rowId.equals("row2")) {
                    checkAttachments(2, complexValueFk, "test_data.txt", "test_data2.txt");
                } else if (rowId.equals("row3")) {
                    checkAttachments(3, complexValueFk);
                } else if (rowId.equals("row4")) {
                    checkAttachments(4, complexValueFk, "test_data2.txt");
                } else {
                    fail();
                }
            }

            Object[] row8 = {"row8", Column.AUTO_NUMBER, "some-data", "row8-memo", Column.AUTO_NUMBER, Column.AUTO_NUMBER};
            t1.addRow(row8);

            ComplexValueForeignKey row8ValFk = (ComplexValueForeignKey) col.getRowValue(row8);
            row8ValFk.addAttachment(null, "test_data.txt", "txt",
                getFileBytes("test_data.txt"), (Date) null, null);
            checkAttachments(row8ValFk.get(), row8ValFk, "test_data.txt");
            row8ValFk.addEncodedAttachment(null, "test_data2.txt", "txt",
                getEncodedFileBytes("test_data2.txt"),
                (Date) null, null);
            checkAttachments(row8ValFk.get(), row8ValFk, "test_data.txt",
                "test_data2.txt");

            Cursor cursor = CursorBuilder.createCursor(t1);
            assertTrue(cursor.findFirstRow(t1.getColumn("id"), "row4"));
            ComplexValueForeignKey row4ValFk = (ComplexValueForeignKey) cursor.getCurrentRowValue(col);
            Attachment a = row4ValFk.addAttachment(null, "test_data.txt", "txt",
                getFileBytes("test_data.txt"),
                (Date) null, null);
            checkAttachments(4, row4ValFk, "test_data2.txt", "test_data.txt");

            a.setFileType("zip");
            a.setFileName("some_data.zip");
            byte[] newBytes = "this is not a zip file".getBytes(StandardCharsets.US_ASCII);
            a.setFileData(newBytes);
            a.update();

            Attachment updated = row4ValFk.getAttachments().get(1);
            assertNotSame(updated, a);
            assertEquals("zip", updated.getFileType());
            assertEquals("some_data.zip", updated.getFileName());
            assertArrayEquals(newBytes, updated.getFileData());
            byte[] encBytes = updated.getEncodedFileData();
            assertEquals(newBytes.length + 28, encBytes.length);
            ByteBuffer bb = PageChannel.wrap(encBytes);
            assertEquals(0, bb.getInt());
            assertTrue(ByteUtil.matchesRange(bb, 28, newBytes));

            updated.delete();
            checkAttachments(4, row4ValFk, "test_data2.txt");
            row4ValFk.getAttachments().get(0).delete();
            checkAttachments(4, row4ValFk);

            assertTrue(cursor.findFirstRow(t1.getColumn("id"), "row2"));
            ComplexValueForeignKey row2ValFk = (ComplexValueForeignKey) cursor.getCurrentRowValue(col);
            row2ValFk.deleteAllValues();
            checkAttachments(2, row2ValFk);

        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void testMultiValues(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Table1");
            Column col = t1.getColumn("multi-value-data");
            assertEquals(ComplexDataType.MULTI_VALUE,
                col.getComplexInfo().getType());

            for (Row row : t1) {
                String rowId = row.getString("id");
                ComplexValueForeignKey complexValueFk =
                    (ComplexValueForeignKey) col.getRowValue(row);

                if (rowId.equals("row1")) {
                    checkMultiValues(1, complexValueFk);
                } else if (rowId.equals("row2")) {
                    checkMultiValues(2, complexValueFk, "value1", "value4");
                } else if (rowId.equals("row3")) {
                    checkMultiValues(3, complexValueFk,
                        "value1", "value2", "value3", "value4");
                } else if (rowId.equals("row4")) {
                    checkMultiValues(4, complexValueFk);
                } else {
                    fail();
                }
            }

            Object[] row8 = {"row8", Column.AUTO_NUMBER, "some-data", "row8-memo", Column.AUTO_NUMBER, Column.AUTO_NUMBER};
            t1.addRow(row8);

            ComplexValueForeignKey row8ValFk = (ComplexValueForeignKey) col.getRowValue(row8);
            row8ValFk.addMultiValue("value1");
            row8ValFk.addMultiValue("value2");
            checkMultiValues(row8ValFk.get(), row8ValFk, "value1", "value2");

            Cursor cursor = CursorBuilder.createCursor(t1);
            assertTrue(cursor.findFirstRow(t1.getColumn("id"), "row2"));
            ComplexValueForeignKey row2ValFk = (ComplexValueForeignKey) cursor.getCurrentRowValue(col);
            SingleValue v = row2ValFk.addMultiValue("value2");
            row2ValFk.addMultiValue("value3");
            checkMultiValues(2, row2ValFk, "value1", "value4", "value2", "value3");

            v.set("value5");
            v.update();
            checkMultiValues(2, row2ValFk, "value1", "value4", "value5", "value3");

            v.delete();
            checkMultiValues(2, row2ValFk, "value1", "value4", "value3");
            row2ValFk.getMultiValues().get(0).delete();
            checkMultiValues(2, row2ValFk, "value4", "value3");
            row2ValFk.getMultiValues().get(1).delete();
            checkMultiValues(2, row2ValFk, "value4");
            row2ValFk.getMultiValues().get(0).delete();
            checkMultiValues(2, row2ValFk);

            assertTrue(cursor.findFirstRow(t1.getColumn("id"), "row3"));
            ComplexValueForeignKey row3ValFk = (ComplexValueForeignKey) cursor.getCurrentRowValue(col);
            row3ValFk.deleteAllValues();
            checkMultiValues(3, row3ValFk);

            // test multi-value col props
            PropertyMap props = col.getProperties();
            assertEquals(Boolean.TRUE, props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP));
            assertEquals("Value List", props.getValue(PropertyMap.ROW_SOURCE_TYPE_PROP));
            assertEquals("\"value1\";\"value2\";\"value3\";\"value4\"",
                props.getValue(PropertyMap.ROW_SOURCE_PROP));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(UNSUPPORTED_FIELDS)
    void testUnsupported(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Test");
            Column col = t1.getColumn("UnknownComplex");
            assertEquals(ComplexDataType.UNSUPPORTED,
                col.getComplexInfo().getType());

            for (Row row : t1) {
                Integer rowId = row.getInt("ID");
                ComplexValueForeignKey complexValueFk =
                    (ComplexValueForeignKey) col.getRowValue(row);

                if (rowId.equals(1)) {
                    checkUnsupportedValues(1, complexValueFk,
                        "RawData[(5) FF FE 62 61  7A]");
                } else if (rowId.equals(2)) {
                    checkUnsupportedValues(2, complexValueFk, "RawData[(5) FF FE 66 6F  6F]", "RawData[(5) FF FE 62 61  7A]");
                } else if (rowId.equals(3)) {
                    checkUnsupportedValues(3, complexValueFk);
                } else {
                    fail();
                }
            }
        }
    }

    private static void checkVersions(
        int cValId, ComplexValueForeignKey complexValueFk,
        String curValue, Object... versionInfos) throws IOException {
        assertEquals(cValId, complexValueFk.get());

        List<Version> versions = complexValueFk.getVersions();
        if (versionInfos.length == 0) {
            assertTrue(versions.isEmpty());
            assertNull(curValue);
        } else {
            assertEquals(versionInfos.length / 2, versions.size());
            assertEquals(curValue, versions.get(0).getValue());
            for (int i = 0; i < versionInfos.length; i += 2) {
                String value = (String) versionInfos[i];
                Date modDate = (Date) versionInfos[i + 1];
                Version v = versions.get(i / 2);
                assertEquals(value, v.getValue());
                TestUtil.assertSameDate(modDate, v.getModifiedDate());
            }
        }
    }

    private static void checkAttachments(
        int cValId, ComplexValueForeignKey complexValueFk,
        String... fileNames) throws IOException {
        assertEquals(cValId, complexValueFk.get());

        List<Attachment> attachments = complexValueFk.getAttachments();
        if (fileNames.length == 0) {
            assertTrue(attachments.isEmpty());
        } else {
            assertEquals(fileNames.length, attachments.size());
            for (int i = 0; i < fileNames.length; i++) {
                String fname = fileNames[i];
                Attachment a = attachments.get(i);
                assertEquals(fname, a.getFileName());
                assertEquals("txt", a.getFileType());
                assertArrayEquals(getFileBytes(fname), a.getFileData());
                assertArrayEquals(getEncodedFileBytes(fname), a.getEncodedFileData());
            }
        }
    }

    private static void checkMultiValues(
        int cValId, ComplexValueForeignKey complexValueFk,
        Object... expectedValues) throws IOException {
        assertEquals(cValId, complexValueFk.get());

        List<SingleValue> values = complexValueFk.getMultiValues();
        if (expectedValues.length == 0) {
            assertTrue(values.isEmpty());
        } else {
            assertEquals(expectedValues.length, values.size());
            for (int i = 0; i < expectedValues.length; i++) {
                Object value = expectedValues[i];
                SingleValue v = values.get(i);
                assertEquals(value, v.get());
            }
        }
    }

    private static void checkUnsupportedValues(
        int cValId, ComplexValueForeignKey complexValueFk,
        String... expectedValues) throws IOException {
        assertEquals(cValId, complexValueFk.get());

        List<UnsupportedValue> values = complexValueFk.getUnsupportedValues();
        if (expectedValues.length == 0) {
            assertTrue(values.isEmpty());
        } else {
            assertEquals(expectedValues.length, values.size());
            for (int i = 0; i < expectedValues.length; i++) {
                String value = expectedValues[i];
                UnsupportedValue v = values.get(i);
                assertEquals(1, v.getValues().size());
                Object rv = v.get("Value");
                assertTrue(ColumnImpl.isRawData(rv));
                assertEquals(value, rv.toString());
            }
        }
    }

    private static byte[] getFileBytes(String fname) {
        if ("test_data.txt".equals(fname)) {
            return TEST_BYTES;
        }
        if ("test_data2.txt".equals(fname)) {
            return TEST2_BYTES;
        }
        throw new JackcessRuntimeException("Unexpected bytes");
    }

    private static byte[] getEncodedFileBytes(String fname) {
        if ("test_data.txt".equals(fname)) {
            return TEST_ENC_BYTES;
        }
        if ("test_data2.txt".equals(fname)) {
            return TEST2_ENC_BYTES;
        }
        throw new JackcessRuntimeException("Unexpected bytes");
    }

    private static byte b(int i) {
        return (byte) i;
    }

    private static byte[] getAsciiBytes(String str) {
        try {
            return str.getBytes(StandardCharsets.US_ASCII);
        } catch (Exception _ex) {
            throw new JackcessRuntimeException(_ex);
        }
    }

}
