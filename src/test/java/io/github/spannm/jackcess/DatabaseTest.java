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

package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.*;
import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.*;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
class DatabaseTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testInvalidTableDefs(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            try {
                DatabaseBuilder.newTable("test").toTable(db);
                fail("created table with no columns?");
            } catch (IllegalArgumentException e) {
                // success
            }

            try {
                DatabaseBuilder.newTable("test")
                    .addColumn(DatabaseBuilder.newColumn("A", DataType.TEXT))
                    .addColumn(DatabaseBuilder.newColumn("a", DataType.MEMO))
                    .toTable(db);
                fail("created table with duplicate column names?");
            } catch (IllegalArgumentException e) {
                // success
            }

            try {
                DatabaseBuilder.newTable("test")
                    .addColumn(DatabaseBuilder.newColumn("A", DataType.TEXT)
                        .withLengthInUnits(352))
                    .toTable(db);
                fail("created table with invalid column length?");
            } catch (IllegalArgumentException e) {
                // success
            }

            try {
                DatabaseBuilder.newTable("test")
                    .addColumn(DatabaseBuilder.newColumn("A_" + createString(70), DataType.TEXT))
                    .toTable(db);
                fail("created table with too long column name?");
            } catch (IllegalArgumentException e) {
                // success
            }

            DatabaseBuilder.newTable("test")
                .addColumn(DatabaseBuilder.newColumn("A", DataType.TEXT))
                .toTable(db);

            try {
                DatabaseBuilder.newTable("Test")
                    .addColumn(DatabaseBuilder.newColumn("A", DataType.TEXT))
                    .toTable(db);
                fail("create duplicate tables?");
            } catch (IllegalArgumentException e) {
                // success
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(DEL)
    void testReadDeletedRows(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Table");
            int rows = 0;
            while (table.getNextRow() != null) {
                rows++;
            }
            assertEquals(2, rows);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testGetColumns(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            List<? extends Column> columns = db.getTable("Table1").getColumns();
            assertEquals(9, columns.size());
            checkColumn(columns, 0, "A", DataType.TEXT);
            checkColumn(columns, 1, "B", DataType.TEXT);
            checkColumn(columns, 2, "C", DataType.BYTE);
            checkColumn(columns, 3, "D", DataType.INT);
            checkColumn(columns, 4, "E", DataType.LONG);
            checkColumn(columns, 5, "F", DataType.DOUBLE);
            checkColumn(columns, 6, "G", DataType.SHORT_DATE_TIME);
            checkColumn(columns, 7, "H", DataType.MONEY);
            checkColumn(columns, 8, "I", DataType.BOOLEAN);
        }
    }

    private static void checkColumn(List<? extends Column> columns, int columnNumber, String name, DataType dataType) {
        Column column = columns.get(columnNumber);
        assertEquals(name, column.getName());
        assertEquals(dataType, column.getType());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testGetNextRow(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            db.setDateTimeType(DateTimeType.DATE);

            assertEquals(4, db.getTableNames().size());
            final Table table = db.getTable("Table1");

            Row row1 = table.getNextRow();
            Row row2 = table.getNextRow();

            if (!"abcdefg".equals(row1.get("A"))) {
                Row tmpRow = row1;
                row1 = row2;
                row2 = tmpRow;
            }

            checkTestDBTable1RowABCDEFG(testDb, table, row1);
            checkTestDBTable1RowA(testDb, table, row2);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCreate(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            assertEquals(0, db.getTableNames().size());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testDeleteCurrentRow(FileFormat fileFormat) throws Exception {
        // make sure correct row is deleted
        try (Database db = createDbMem(fileFormat)) {
            createTestTable(db);
            Map<String, Object> row1 = createTestRowMap("Tim1");
            Map<String, Object> row2 = createTestRowMap("Tim2");
            Map<String, Object> row3 = createTestRowMap("Tim3");
            Table table = db.getTable("Test");
            List<Map<String, Object>> rows = List.of(row1, row2, row3);
            table.addRowsFromMaps(rows);
            assertRowCount(3, table);

            table.reset();
            table.getNextRow();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();

            table.reset();

            Map<String, Object> outRow = table.getNextRow();
            assertEquals("Tim1", outRow.get("A"));
            outRow = table.getNextRow();
            assertEquals("Tim3", outRow.get("A"));
            assertRowCount(2, table);
        }

        try (Database db = createDbMem(fileFormat)) { // test multi row delete/add
            createTestTable(db);
            Object[] row = createTestRow();
            Table table = db.getTable("Test");
            for (int i = 0; i < 10; i++) {
                row[3] = i;
                table.addRow(row);
            }
            row[3] = 1974;
            assertRowCount(10, table);
            table.reset();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();
            assertRowCount(9, table);
            table.reset();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();
            assertRowCount(8, table);
            table.reset();
            for (int i = 0; i < 8; i++) {
                table.getNextRow();
            }
            table.getDefaultCursor().deleteCurrentRow();
            assertRowCount(7, table);
            table.addRow(row);
            assertRowCount(8, table);
            table.reset();
            for (int i = 0; i < 3; i++) {
                table.getNextRow();
            }
            table.getDefaultCursor().deleteCurrentRow();
            assertRowCount(7, table);
            table.reset();
            assertEquals(2, table.getNextRow().get("D"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testDeleteRow(FileFormat fileFormat) throws Exception {
        // make sure correct row is deleted
        try (
        Database db = createDbMem(fileFormat)) {
            createTestTable(db);
            Table table = db.getTable("Test");
            for (int i = 0; i < 10; i++) {
                table.addRowFromMap(createTestRowMap("Tim" + i));
            }
            assertRowCount(10, table);

            table.reset();

            List<Row> rows = toList(table);

            Row r1 = rows.remove(7);
            Row r2 = rows.remove(3);
            assertEquals(8, rows.size());

            assertSame(r2, table.deleteRow(r2));
            assertSame(r1, table.deleteRow(r1));

            assertTable(rows, table);

            table.deleteRow(r2);
            table.deleteRow(r1);

            assertTable(rows, table);
        }
    }

    @Test
    void testMissingFile() {
        File bogusFile = new File("fooby-dooby.mdb");
        assertFalse(bogusFile.exists());
        DatabaseBuilder dbb = DatabaseBuilder.newDatabase(bogusFile).withReadOnly(true).withAutoSync(getTestAutoSync());
        assertThrows(FileNotFoundException.class, () -> {
            try (Database ignored = dbb.open()) {}
        });
        assertFalse(bogusFile.exists());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(DEL_COL)
    void testReadWithDeletedCols(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Table1");

            Map<String, Object> expectedRow0 = new LinkedHashMap<>();
            expectedRow0.put("id", 0);
            expectedRow0.put("id2", 2);
            expectedRow0.put("data", "foo");
            expectedRow0.put("data2", "foo2");

            Map<String, Object> expectedRow1 = new LinkedHashMap<>();
            expectedRow1.put("id", 3);
            expectedRow1.put("id2", 5);
            expectedRow1.put("data", "bar");
            expectedRow1.put("data2", "bar2");

            int rowNum = 0;
            Map<String, Object> row = null;
            while ((row = table.getNextRow()) != null) {
                if (rowNum == 0) {
                    assertEquals(expectedRow0, row);
                } else if (rowNum == 1) {
                    assertEquals(expectedRow1, row);
                } else if (rowNum >= 2) {
                    fail("should only have 2 rows");
                }
                rowNum++;
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCurrency(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = DatabaseBuilder.newTable("test")
                .addColumn(DatabaseBuilder.newColumn("A", DataType.MONEY))
                .toTable(db);

            table.addRow(new BigDecimal("-2341234.03450"));
            table.addRow(37L);
            table.addRow("10000.45");

            table.reset();

            List<Object> foundValues = new ArrayList<>();
            Map<String, Object> row = null;
            while ((row = table.getNextRow()) != null) {
                foundValues.add(row.get("A"));
            }

            assertEquals(List.of(
                new BigDecimal("-2341234.0345"),
                new BigDecimal("37.0000"),
                new BigDecimal("10000.4500")),
                foundValues);

            assertThrows(IOException.class, () -> table.addRow(new BigDecimal("342523234145343543.3453")));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testGUID(FileFormat fileFormat) throws Exception {
        Database db = createDbMem(fileFormat);

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(DatabaseBuilder.newColumn("A", DataType.GUID))
            .toTable(db);

        table.addRow("{32A59F01-AA34-3E29-453F-4523453CD2E6}");
        table.addRow("{32a59f01-aa34-3e29-453f-4523453cd2e6}");
        table.addRow("{11111111-1111-1111-1111-111111111111}");
        table.addRow("   {FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}   ");
        table.addRow(UUID.fromString("32a59f01-1234-3e29-4aaf-4523453cd2e6"));

        table.reset();

        List<Object> foundValues = new ArrayList<>();
        Map<String, Object> row = null;
        while ((row = table.getNextRow()) != null) {
            foundValues.add(row.get("A"));
        }

        assertEquals(List.of(
            "{32A59F01-AA34-3E29-453F-4523453CD2E6}",
            "{32A59F01-AA34-3E29-453F-4523453CD2E6}",
            "{11111111-1111-1111-1111-111111111111}",
            "{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}",
            "{32A59F01-1234-3E29-4AAF-4523453CD2E6}"),
            foundValues);

        assertThrows(IOException.class, () -> table.addRow("3245234"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testNumeric(FileFormat fileFormat) throws Exception {
        Database db = createDbMem(fileFormat);

        ColumnBuilder col = DatabaseBuilder.newColumn("A", DataType.NUMERIC)
            .withScale(4).withPrecision(8).toColumn();
        assertTrue(col.getType().isVariableLength());

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(col)
            .addColumn(DatabaseBuilder.newColumn("B", DataType.NUMERIC)
                .withScale(8).withPrecision(28))
            .toTable(db);

        table.addRow(new BigDecimal("-1234.03450"),
            new BigDecimal("23923434453436.36234219"));
        table.addRow(37L, 37L);
        table.addRow("1000.45", "-3452345321000");

        table.reset();

        List<Object> foundSmallValues = new ArrayList<>();
        List<Object> foundBigValues = new ArrayList<>();
        Map<String, Object> row = null;
        while ((row = table.getNextRow()) != null) {
            foundSmallValues.add(row.get("A"));
            foundBigValues.add(row.get("B"));
        }

        assertEquals(List.of(
            new BigDecimal("-1234.0345"),
            new BigDecimal("37.0000"),
            new BigDecimal("1000.4500")),
            foundSmallValues);
        assertEquals(List.of(
            new BigDecimal("23923434453436.36234219"),
            new BigDecimal("37.00000000"),
            new BigDecimal("-3452345321000.00000000")),
            foundBigValues);

        assertThrows(IOException.class, () -> table.addRow(new BigDecimal("3245234.234"),
            new BigDecimal("3245234.234")));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(FIXED_NUMERIC)
    void testFixedNumeric(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            Table t = db.getTable("test");

            boolean first = true;
            for (Column col : t.getColumns()) {
                if (first) {
                    assertTrue(col.isVariableLength());
                    assertEquals(DataType.MEMO, col.getType());
                    first = false;
                } else {
                    assertFalse(col.isVariableLength());
                    assertEquals(DataType.NUMERIC, col.getType());
                }
            }

            Map<String, Object> row = t.getNextRow();
            assertEquals("some data", row.get("col1"));
            assertEquals(BigDecimal.ONE, row.get("col2"));
            assertEquals(BigDecimal.ZERO, row.get("col3"));
            assertEquals(BigDecimal.ZERO, row.get("col4"));
            assertEquals(new BigDecimal("4"), row.get("col5"));
            assertEquals(new BigDecimal("-1"), row.get("col6"));
            assertEquals(BigDecimal.ONE, row.get("col7"));

            Object[] tmpRow = new Object[] {"foo", BigDecimal.ONE, new BigDecimal(3), new BigDecimal("13"), new BigDecimal("-17"), BigDecimal.ZERO, new BigDecimal("8734")};
            t.addRow(tmpRow);
            t.reset();

            t.getNextRow();
            row = t.getNextRow();
            assertEquals(tmpRow[0], row.get("col1"));
            assertEquals(tmpRow[1], row.get("col2"));
            assertEquals(tmpRow[2], row.get("col3"));
            assertEquals(tmpRow[3], row.get("col4"));
            assertEquals(tmpRow[4], row.get("col5"));
            assertEquals(tmpRow[5], row.get("col6"));
            assertEquals(tmpRow[6], row.get("col7"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testMultiPageTableDef(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            List<? extends Column> columns = db.getTable("Table2").getColumns();
            assertEquals(89, columns.size());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(OVERFLOW)
    void testOverflow(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Table1");

            // 7 rows, 3 and 5 are overflow
            table.getNextRow();
            table.getNextRow();

            Map<String, Object> row = table.getNextRow();
            assertEquals(Arrays.asList(null, "row3col3", null, null, null, null, null, "row3col9", null),
                new ArrayList<>(row.values()));

            table.getNextRow();

            row = table.getNextRow();
            assertEquals(Arrays.asList(null, "row5col2", null, null, null, null, null, null, null),
                new ArrayList<>(row.values()));

            table.reset();
            assertRowCount(7, table);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(PROMOTION)
    void testUsageMapPromotion(TestDb testDb) throws Exception {

        try (Database db = testDb.openMem()) {
            Table t = db.getTable("jobDB1");

            assertTrue(((TableImpl) t).getOwnedPagesCursor().getUsageMap().toString()
                .startsWith("InlineHandler"));

            String lval = createNonAsciiString(255); // "--255 chars long text--";

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                for (int i = 0; i < 1000; i++) {
                    t.addRow(i, 13, 57, lval, lval, lval, lval, lval, lval, 47.0d);
                }
            } finally {
                ((DatabaseImpl) db).getPageChannel().finishWrite();
            }

            Set<Integer> ids = t.stream()
                .map(r -> r.getInt("ID"))
                .collect(Collectors.toSet());
            assertEquals(1000, ids.size());

            assertTrue(((TableImpl) t).getOwnedPagesCursor().getUsageMap().toString()
                .startsWith("ReferenceHandler"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testLargeTableDef(FileFormat fileFormat) throws Exception {
        Database db = createDbMem(fileFormat);

        final int numColumns = 90;

        List<ColumnBuilder> columns = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        for (int i = 0; i < numColumns; i++) {
            String colName = "MyColumnName" + i;
            colNames.add(colName);
            columns.add(DatabaseBuilder.newColumn(colName, DataType.TEXT).toColumn());
        }

        Table t = DatabaseBuilder.newTable("test")
            .addColumns(columns)
            .toTable(db);

        List<String> row = new ArrayList<>();
        Map<String, Object> expectedRowData = new LinkedHashMap<>();
        for (int i = 0; i < numColumns; i++) {
            String value = i + " some row data";
            row.add(value);
            expectedRowData.put(colNames.get(i), value);
        }

        t.addRow(row.toArray());

        t.reset();
        assertEquals(expectedRowData, t.getNextRow());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testWriteAndReadDate(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            db.setDateTimeType(DateTimeType.DATE);

            Table table = DatabaseBuilder.newTable("test")
                .addColumn(DatabaseBuilder.newColumn("name", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("date", DataType.SHORT_DATE_TIME))
                .toTable(db);

            // since jackcess does not really store millis, shave them off before
            // storing the current date/time
            long curTimeNoMillis = System.currentTimeMillis() / 1000L;
            curTimeNoMillis *= 1000L;

            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            List<Date> dates = new ArrayList<>(Arrays.asList(
                df.parse("19801231 00:00:00"),
                df.parse("19930513 14:43:27"),
                null,
                df.parse("20210102 02:37:00"),
                new Date(curTimeNoMillis)));

            Calendar c = Calendar.getInstance();
            for (int year = 1801; year < 2050; year += 3) {
                for (int month = 0; month <= 12; ++month) {
                    for (int day = 1; day < 29; day += 3) {
                        c.clear();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        dates.add(c.getTime());
                    }
                }
            }

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                for (Date d : dates) {
                    table.addRow("row " + d, d);
                }
            } finally {
                ((DatabaseImpl) db).getPageChannel().finishWrite();
            }

            List<Date> foundDates = table.stream()
                .map(r -> r.getDate("date"))
                .collect(Collectors.toList());

            assertEquals(dates.size(), foundDates.size());
            for (int i = 0; i < dates.size(); i++) {
                Date expected = dates.get(i);
                Date found = foundDates.get(i);
                assertSameDate(expected, found);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testAncientDatesWrite(FileFormat fileFormat) throws Exception {
        SimpleDateFormat sdf = DatabaseBuilder.createDateFormat("yyyy-MM-dd");

        List<String> dates = List.of("1582-10-15", "1582-10-14", "1492-01-10", "1392-01-10");

        Database db = createDbMem(fileFormat);
        db.setDateTimeType(DateTimeType.DATE);

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(DatabaseBuilder.newColumn("name", DataType.TEXT))
            .addColumn(DatabaseBuilder.newColumn("date", DataType.SHORT_DATE_TIME))
            .toTable(db);

        for (String dateStr : dates) {
            Date d = sdf.parse(dateStr);
            table.addRow("row " + dateStr, d);
        }

        List<String> foundDates = table.stream()
            .map(r -> sdf.format(r.getDate("date")))
            .collect(Collectors.toList());

        assertEquals(dates, foundDates);
    }

    /**
     * Test ancient date handling against test database {@code oldDates*.accdb}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(OLD_DATES)
    void testAncientDatesRead(TestDb testDb) throws Exception {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        SimpleDateFormat sdf = DatabaseBuilder.createDateFormat("yyyy-MM-dd");
        sdf.getCalendar().setTimeZone(tz);

        List<String> dates = List.of("1582-10-15", "1582-10-14", "1492-01-10", "1392-01-10");

        try (Database db = testDb.openCopy()) {
            db.setTimeZone(tz); // explicitly set database time zone
            db.setDateTimeType(DateTimeType.DATE);

            Table t = db.getTable("Table1");

            List<String> foundDates = new ArrayList<>();
            for (Row row : t) {
                foundDates.add(sdf.format(row.getDate("DateField")));
            }

            assertEquals(dates, foundDates);
        }

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testSystemTable(FileFormat fileFormat) throws Exception {
        Database db = createDbMem(fileFormat);

        Set<String> sysTables = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sysTables.addAll(List.of("MSysObjects", "MSysQueries", "MSysACES", "MSysRelationships"));

        if (fileFormat == FileFormat.GENERIC_JET4) {
            assertNull(db.getSystemTable("MSysAccessObjects"), "file format: " + fileFormat);
        } else if (fileFormat.ordinal() < FileFormat.V2003.ordinal()) {
            assertNotNull(db.getSystemTable("MSysAccessObjects"), "file format: " + fileFormat);
            sysTables.add("MSysAccessObjects");
        } else {
            // v2003+ template files have no "MSysAccessObjects" table
            assertNull(db.getSystemTable("MSysAccessObjects"), "file format: " + fileFormat);
            sysTables.addAll(List.of("MSysNavPaneGroupCategories", "MSysNavPaneGroups", "MSysNavPaneGroupToObjects", "MSysNavPaneObjectIDs", "MSysAccessStorage"));
            if (fileFormat.ordinal() >= FileFormat.V2007.ordinal()) {
                sysTables.addAll(List.of("MSysComplexColumns", "MSysComplexType_Attachment", "MSysComplexType_Decimal", "MSysComplexType_GUID", "MSysComplexType_IEEEDouble",
                    "MSysComplexType_IEEESingle", "MSysComplexType_Long", "MSysComplexType_Short", "MSysComplexType_Text", "MSysComplexType_UnsignedByte"));
            }
            if (fileFormat.ordinal() >= FileFormat.V2010.ordinal()) {
                sysTables.add("f_12D7448B56564D8AAE333BCC9B3718E5_Data");
                sysTables.add("MSysResources");
            }
            if (fileFormat.ordinal() >= FileFormat.V2019.ordinal()) {
                sysTables.remove("f_12D7448B56564D8AAE333BCC9B3718E5_Data");
                sysTables.add("f_8FA5340F56044616AE380F64A2FEC135_Data");
                sysTables.add("MSysWSDPCacheComplexColumnMapping");
                sysTables.add("MSysWSDPChangeTokenMapping");
                sysTables.add("MSysWSDPRelationshipMapping");
            }
        }

        assertEquals(sysTables, db.getSystemTableNames());

        assertNotNull(db.getSystemTable("MSysObjects"));
        assertNotNull(db.getSystemTable("MSysQueries"));
        assertNotNull(db.getSystemTable("MSysACES"));
        assertNotNull(db.getSystemTable("MSysRelationships"));

        assertNull(db.getSystemTable("MSysBogus"));

        TableMetaData tmd = db.getTableMetaData("MSysObjects");
        assertEquals("MSysObjects", tmd.getName());
        assertFalse(tmd.isLinked());
        assertTrue(tmd.isSystem());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(FIXED_TEXT)
    void testFixedText(TestDb testDb) throws Exception {

        try (Database db = testDb.openCopy()) {
            Table t = db.getTable("users");
            Column c = t.getColumn("c_flag_");
            assertEquals(DataType.TEXT, c.getType());
            assertFalse(c.isVariableLength());
            assertEquals(2, c.getLength());

            Map<String, Object> row = t.getNextRow();
            assertEquals("N", row.get("c_flag_"));

            t.addRow(3, "testFixedText", "boo", "foo", "bob", 3, 5, 9, "Y",
                new Date());

            t.getNextRow();
            row = t.getNextRow();
            assertEquals("testFixedText", row.get("c_user_login"));
            assertEquals("Y", row.get("c_flag_"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testDbSortOrder(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            assertEquals(((DatabaseImpl) db).getFormat().DEFAULT_SORT_ORDER,
                ((DatabaseImpl) db).getDefaultSortOrder());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(UNSUPPORTED_FIELDS)
    void testUnsupportedColumns(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table t = db.getTable("Test");
            Column varCol = t.getColumn("UnknownVar");
            assertEquals(DataType.UNSUPPORTED_VARLEN, varCol.getType());
            Column fixCol = t.getColumn("UnknownFix");
            assertEquals(DataType.UNSUPPORTED_FIXEDLEN, fixCol.getType());

            List<String> varVals = Arrays.asList(
                "RawData[(10) FF FE 73 6F  6D 65 64 61  74 61]",
                "RawData[(12) FF FE 6F 74  68 65 72 20  64 61 74 61]",
                null);
            List<String> fixVals = Arrays.asList("RawData[(4) 37 00 00 00]",
                "RawData[(4) F3 FF FF FF]",
                "RawData[(4) 02 00 00 00]");

            int idx = 0;
            for (Map<String, Object> row : t) {
                checkRawValue(varVals.get(idx), varCol.getRowValue(row));
                checkRawValue(fixVals.get(idx), fixCol.getRowValue(row));
                idx++;
            }
        }
    }

    static List<Table> getTables(Iterable<Table> tableIter) {
        List<Table> tableList = new ArrayList<>();
        for (Table t : tableIter) {
            tableList.add(t);
        }
        return tableList;
    }

    @Test
    void testTimeZone() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        doTestTimeZone(tz);

        tz = TimeZone.getTimeZone("Australia/Sydney");
        doTestTimeZone(tz);
    }

    private static void doTestTimeZone(final TimeZone tz) throws Exception {
        ColumnImpl col = new ColumnImpl(null, null, DataType.SHORT_DATE_TIME, 0, 0, 0) {
            @Override
            public TimeZone getTimeZone() {
                return tz;
            }

            @Override
            public ZoneId getZoneId() {
                return null;
            }

            @Override
            public ColumnImpl.DateTimeFactory getDateTimeFactory() {
                return getDateTimeFactory(DateTimeType.DATE);
            }
        };

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");
        df.setTimeZone(tz);

        long startDate = df.parse("2012.01.01").getTime();
        long endDate = df.parse("2013.01.01").getTime();

        Calendar curCal = Calendar.getInstance(tz);
        curCal.setTimeInMillis(startDate);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        sdf.setTimeZone(tz);

        while (curCal.getTimeInMillis() < endDate) {
            Date curDate = curCal.getTime();
            Date newDate = new Date(col.fromDateDouble(col.toDateDouble(curDate)));
            if (curDate.getTime() != newDate.getTime()) {
                assertEquals(sdf.format(curDate), sdf.format(newDate));
            }
            curCal.add(Calendar.MINUTE, 30);
        }
    }

    @Test
    void testToString() {
        RowImpl row = new RowImpl(new RowIdImpl(1, 1));
        row.put("id", 37);
        row.put("data", null);
        assertEquals("Row[1:1][{id=37,data=<null>}]", row.toString());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testIterateTableNames1(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Set<String> names = new HashSet<>();
            int sysCount = 0;
            for (TableMetaData tmd : db.newTableMetaDataIterable()) {
                if (tmd.isSystem()) {
                    sysCount++;
                    continue;
                }
                assertFalse(tmd.isLinked());
                assertNull(tmd.getLinkedTableName());
                assertNull(tmd.getLinkedDbName());
                names.add(tmd.getName());
            }

            assertTrue(sysCount > 4);
            assertEquals(Set.of("Table1", "Table2", "Table3", "Table4"), names);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(LINKED)
    void testIterateTableNames2(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Set<String> names = new HashSet<>();
            for (TableMetaData tmd : db.newTableMetaDataIterable()) {
                if (tmd.isSystem()) {
                    continue;
                }
                if ("Table1".equals(tmd.getName())) {
                    assertFalse(tmd.isLinked());
                    assertNull(tmd.getLinkedTableName());
                    assertNull(tmd.getLinkedDbName());
                } else {
                    assertTrue(tmd.isLinked());
                    assertEquals("Table1", tmd.getLinkedTableName());
                    assertEquals("Z:\\jackcess_test\\linkeeTest.accdb", tmd.getLinkedDbName());
                }
                names.add(tmd.getName());
            }

            assertEquals(Set.of("Table1", "Table2"), names);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testTableDates(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Table1");
            String expectedCreateDate = null;
            String expectedUpdateDate = null;
            if (testDb.getExpectedFileFormat() == FileFormat.V1997) {
                expectedCreateDate = "2010-03-05T14:48:26.420";
                expectedUpdateDate = "2010-03-05T14:48:26.607";
            } else {
                expectedCreateDate = "2004-05-28T17:51:48.701";
                expectedUpdateDate = "2006-07-24T09:56:19.701";
            }
            assertEquals(expectedCreateDate, table.getCreatedDate().toString());
            assertEquals(expectedUpdateDate, table.getUpdatedDate().toString());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void testBrokenIndex(TestDb testDb) throws Exception {
        try (Database db = new DatabaseBuilder()
            .withFile(testDb.getFile())
            .withReadOnly(true).withIgnoreBrokenSystemCatalogIndex(true).open()) {
            Table test = db.getTable("Table1");
            assertNotNull(test);
            verifyFinderType(db, "FallbackTableFinder");
        }
        try (Database db = testDb.openMem()) {
            Table test = db.getTable("Table1");
            assertNotNull(test);
            verifyFinderType(db, "DefaultTableFinder");
        }
    }

    private static void verifyFinderType(Database db, String clazzName) throws Exception {
        java.lang.reflect.Field f = db.getClass().getDeclaredField("_tableFinder");
        f.setAccessible(true);
        Object finder = f.get(db);
        assertNotNull(finder);
        assertEquals(clazzName, finder.getClass().getSimpleName());
    }

    private static void checkRawValue(String expected, Object val) {
        if (expected != null) {
            assertTrue(ColumnImpl.isRawData(val));
            assertEquals(expected, val.toString());
        } else {
            assertNull(val);
        }
    }
}
