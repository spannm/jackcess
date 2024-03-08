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

import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.RowIdImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDB;
import io.github.spannm.jackcess.util.CaseInsensitiveColumnMatcher;
import io.github.spannm.jackcess.util.ColumnMatcher;
import io.github.spannm.jackcess.util.SimpleColumnMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author James Ahlborn
 */
class CursorTest extends AbstractBaseTest {

    static final List<TestDB> INDEX_CURSOR_DBS = TestDB.getSupportedTestDbs(Basename.INDEX_CURSOR);

    @BeforeEach
    void setUp() {
        setTestAutoSync(false);
    }

    @AfterEach
    void tearDown() {
        clearTestAutoSync();
    }

    private static List<Map<String, Object>> createTestTableData() {
        List<Map<String, Object>> expectedRows =
            new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i));
        }
        return expectedRows;
    }

    private static List<Map<String, Object>> createTestTableData(int startIdx, int endIdx) {
        List<Map<String, Object>> expectedRows = createTestTableData();
        expectedRows.subList(endIdx, expectedRows.size()).clear();
        expectedRows.subList(0, startIdx).clear();
        return expectedRows;
    }

    private static Database createTestTable(FileFormat fileFormat) throws Exception {
        Database db = createMem(fileFormat);

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
            .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
            .toTable(db);

        for (Map<String, Object> row : createTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    private static List<Map<String, Object>> createUnorderedTestTableData() {
        List<Map<String, Object>> expectedRows =
            new ArrayList<>();
        int[] ids = new int[] {3, 7, 6, 1, 2, 9, 0, 5, 4, 8};
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i));
        }
        return expectedRows;
    }

    static Database createTestIndexTable(TestDB indexCursorDB) throws Exception {
        Database db = indexCursorDB.openMem();

        Table table = db.getTable("test");

        for (Map<String, Object> row : createUnorderedTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    private static List<Map<String, Object>> createDupeTestTableData() {
        List<Map<String, Object>> expectedRows =
            new ArrayList<>();
        int[] ids = new int[] {3, 7, 6, 1, 2, 9, 0, 5, 4, 8};
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i % 3));
        }
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i % 5));
        }
        return expectedRows;
    }

    private static Database createDupeTestTable(FileFormat fileFormat) throws Exception {
        Database db = createMem(fileFormat);

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
            .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
            .toTable(db);

        for (Map<String, Object> row : createDupeTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    static Database createDupeTestTable(TestDB indexCursorDB) throws Exception {
        Database db = indexCursorDB.openMem();

        Table table = db.getTable("test");

        for (Map<String, Object> row : createDupeTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    private static Cursor createIndexSubRangeCursor(Table table,
        Index idx,
        int type) throws Exception {
        return table.newCursor()
            .withIndex(idx)
            .withStartEntry(3 - type)
            .withStartRowInclusive(type == 0)
            .withEndEntry(8 + type)
            .withEndRowInclusive(type == 0)
            .toCursor();
    }

    @Test
    void testRowId() {
        // test special cases
        RowIdImpl rowId1 = new RowIdImpl(1, 2);
        RowIdImpl rowId2 = new RowIdImpl(1, 3);
        RowIdImpl rowId3 = new RowIdImpl(2, 1);

        List<RowIdImpl> sortedRowIds =
            new ArrayList<>(new TreeSet<>(
                List.of(rowId1, rowId2, rowId3, RowIdImpl.FIRST_ROW_ID,
                    RowIdImpl.LAST_ROW_ID)));

        assertEquals(List.of(RowIdImpl.FIRST_ROW_ID, rowId1, rowId2, rowId3,
            RowIdImpl.LAST_ROW_ID),
            sortedRowIds);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testSimple(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestSimple(cursor, null);
        }
    }

    private static void doTestSimple(Cursor cursor,
        List<Map<String, Object>> expectedRows) {
        if (expectedRows == null) {
            expectedRows = createTestTableData();
        }

        List<Map<String, Object>> foundRows =
            new ArrayList<>();
        for (Map<String, Object> row : cursor) {
            foundRows.add(row);
        }
        assertEquals(expectedRows, foundRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testMove(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestMove(cursor, null);
        }
    }

    private static void doTestMove(Cursor cursor,
        List<Map<String, Object>> expectedRows) throws Exception {
        if (expectedRows == null) {
            expectedRows = createTestTableData();
        }
        expectedRows.subList(1, 4).clear();

        List<Map<String, Object>> foundRows =
            new ArrayList<>();
        assertTrue(cursor.isBeforeFirst());
        assertFalse(cursor.isAfterLast());
        foundRows.add(cursor.getNextRow());
        assertEquals(3, cursor.moveNextRows(3));
        assertFalse(cursor.isBeforeFirst());
        assertFalse(cursor.isAfterLast());

        Map<String, Object> expectedRow = cursor.getCurrentRow();
        Cursor.Savepoint savepoint = cursor.getSavepoint();
        assertEquals(2, cursor.movePreviousRows(2));
        assertEquals(2, cursor.moveNextRows(2));
        assertTrue(cursor.moveToNextRow());
        assertTrue(cursor.moveToPreviousRow());
        assertEquals(expectedRow, cursor.getCurrentRow());

        while (cursor.moveToNextRow()) {
            foundRows.add(cursor.getCurrentRow());
        }
        assertEquals(expectedRows, foundRows);
        assertFalse(cursor.isBeforeFirst());
        assertTrue(cursor.isAfterLast());

        assertEquals(0, cursor.moveNextRows(3));

        cursor.beforeFirst();
        assertTrue(cursor.isBeforeFirst());
        assertFalse(cursor.isAfterLast());

        cursor.afterLast();
        assertFalse(cursor.isBeforeFirst());
        assertTrue(cursor.isAfterLast());

        cursor.restoreSavepoint(savepoint);
        assertEquals(expectedRow, cursor.getCurrentRow());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testMoveNoReset(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestMoveNoReset(cursor);
        }
    }

    private static void doTestMoveNoReset(Cursor cursor) {
        List<Map<String, Object>> expectedRows = createTestTableData();
        List<Map<String, Object>> foundRows = new ArrayList<>();

        Iterator<Row> iter = cursor.newIterable().iterator();

        for (int i = 0; i < 6; i++) {
            foundRows.add(iter.next());
        }

        iter = cursor.newIterable().reset(false).reverse().iterator();
        iter.next();
        Map<String, Object> row = iter.next();
        assertEquals(expectedRows.get(4), row);

        iter = cursor.newIterable().reset(false).iterator();
        iter.next();
        row = iter.next();
        assertEquals(expectedRows.get(5), row);
        iter.next();

        iter = cursor.newIterable().reset(false).iterator();
        for (int i = 6; i < 10; i++) {
            foundRows.add(iter.next());
        }

        assertEquals(expectedRows, foundRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testSearch(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestSearch(table, cursor, null, 42, -13);
        }
    }

    private static void doTestSearch(Table table, Cursor cursor, Index index, Integer... outOfRangeValues) throws Exception {
        assertTrue(cursor.findFirstRow(table.getColumn("id"), 3));
        assertEquals(createExpectedRow("id", 3,
            "value", "data" + 3),
            cursor.getCurrentRow());

        assertTrue(cursor.findFirstRow(createExpectedRow(
            "id", 6,
            "value", "data" + 6)));
        assertEquals(createExpectedRow("id", 6,
            "value", "data" + 6),
            cursor.getCurrentRow());

        assertFalse(cursor.findFirstRow(createExpectedRow(
            "id", 8,
            "value", "data" + 13)));
        assertFalse(cursor.findFirstRow(table.getColumn("id"), 13));
        assertEquals(createExpectedRow("id", 6,
            "value", "data" + 6),
            cursor.getCurrentRow());

        assertTrue(cursor.findFirstRow(createExpectedRow(
            "value", "data" + 7)));
        assertEquals(createExpectedRow("id", 7,
            "value", "data" + 7),
            cursor.getCurrentRow());

        assertTrue(cursor.findFirstRow(table.getColumn("value"), "data" + 4));
        assertEquals(createExpectedRow("id", 4,
            "value", "data" + 4),
            cursor.getCurrentRow());

        for (Integer outOfRangeValue : outOfRangeValues) {
            assertFalse(cursor.findFirstRow(table.getColumn("id"),
                outOfRangeValue));
            assertFalse(cursor.findFirstRow(table.getColumn("value"),
                "data" + outOfRangeValue));
            assertFalse(cursor.findFirstRow(createExpectedRow(
                "id", outOfRangeValue,
                "value", "data" + outOfRangeValue)));
        }

        assertEquals("data" + 5,
            CursorBuilder.findValue(table,
                table.getColumn("value"),
                table.getColumn("id"), 5));
        assertEquals(createExpectedRow("id", 5,
            "value", "data" + 5),
            CursorBuilder.findRow(table,
                createExpectedRow("id", 5)));
        if (index != null) {
            assertEquals("data" + 5,
                CursorBuilder.findValue(index,
                    table.getColumn("value"),
                    table.getColumn("id"), 5));
            assertEquals(createExpectedRow("id", 5,
                "value", "data" + 5),
                CursorBuilder.findRow(index,
                    createExpectedRow("id", 5)));

            assertNull(CursorBuilder.findValue(index,
                table.getColumn("value"),
                table.getColumn("id"),
                -17));
            assertNull(CursorBuilder.findRow(index,
                createExpectedRow("id", 13)));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testReverse(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestReverse(cursor, null);
        }
    }

    private static void doTestReverse(Cursor cursor, List<Map<String, Object>> expectedRows) {
        if (expectedRows == null) {
            expectedRows = createTestTableData();
        }
        Collections.reverse(expectedRows);

        List<Map<String, Object>> foundRows = new ArrayList<>();
        for (Map<String, Object> row : cursor.newIterable().reverse()) {
            foundRows.add(row);
        }
        assertEquals(expectedRows, foundRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testLiveAddition(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");

            Cursor cursor1 = CursorBuilder.createCursor(table);
            Cursor cursor2 = CursorBuilder.createCursor(table);
            doTestLiveAddition(table, cursor1, cursor2, 11);
        }
    }

    private static void doTestLiveAddition(Table table,
        Cursor cursor1,
        Cursor cursor2,
        Integer newRowNum) throws Exception {
        cursor1.moveNextRows(11);
        cursor2.moveNextRows(11);

        assertTrue(cursor1.isAfterLast());
        assertTrue(cursor2.isAfterLast());

        table.addRow(newRowNum, "data" + newRowNum);
        Map<String, Object> expectedRow =
            createExpectedRow("id", newRowNum, "value", "data" + newRowNum);

        assertFalse(cursor1.isAfterLast());
        assertFalse(cursor2.isAfterLast());

        assertEquals(expectedRow, cursor1.getCurrentRow());
        assertEquals(expectedRow, cursor2.getCurrentRow());
        assertFalse(cursor1.moveToNextRow());
        assertFalse(cursor2.moveToNextRow());
        assertTrue(cursor1.isAfterLast());
        assertTrue(cursor2.isAfterLast());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testLiveDeletion(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");

            Cursor cursor1 = CursorBuilder.createCursor(table);
            Cursor cursor2 = CursorBuilder.createCursor(table);
            Cursor cursor3 = CursorBuilder.createCursor(table);
            Cursor cursor4 = CursorBuilder.createCursor(table);
            doTestLiveDeletion(cursor1, cursor2, cursor3, cursor4, 1);
        }
    }

    private static void doTestLiveDeletion(Cursor cursor1, Cursor cursor2, Cursor cursor3, Cursor cursor4, int firstValue) throws Exception {
        assertEquals(2, cursor1.moveNextRows(2));
        assertEquals(3, cursor2.moveNextRows(3));
        assertEquals(3, cursor3.moveNextRows(3));
        assertEquals(4, cursor4.moveNextRows(4));

        Map<String, Object> expectedPrevRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);
        firstValue++;
        Map<String, Object> expectedDeletedRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);
        firstValue++;
        Map<String, Object> expectedNextRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);

        assertEquals(expectedDeletedRow, cursor2.getCurrentRow());
        assertEquals(expectedDeletedRow, cursor3.getCurrentRow());

        assertFalse(cursor2.isCurrentRowDeleted());
        assertFalse(cursor3.isCurrentRowDeleted());

        cursor2.deleteCurrentRow();

        assertTrue(cursor2.isCurrentRowDeleted());
        assertTrue(cursor3.isCurrentRowDeleted());

        assertEquals(expectedNextRow, cursor1.getNextRow());
        assertEquals(expectedNextRow, cursor2.getNextRow());
        assertEquals(expectedNextRow, cursor3.getNextRow());

        assertEquals(expectedPrevRow, cursor3.getPreviousRow());

        assertTrue(cursor3.moveToNextRow());
        cursor3.deleteCurrentRow();
        assertTrue(cursor3.isCurrentRowDeleted());

        firstValue += 2;
        expectedNextRow =
            createExpectedRow("id", firstValue, "value", "data" + firstValue);
        assertTrue(cursor3.moveToNextRow());
        assertEquals(expectedNextRow, cursor3.getNextRow());

        cursor1.beforeFirst();
        assertTrue(cursor1.moveToNextRow());
        cursor1.deleteCurrentRow();
        assertFalse(cursor1.isBeforeFirst());
        assertFalse(cursor1.isAfterLast());
        assertFalse(cursor1.moveToPreviousRow());
        assertTrue(cursor1.isBeforeFirst());
        assertFalse(cursor1.isAfterLast());

        cursor1.afterLast();
        assertTrue(cursor1.moveToPreviousRow());
        cursor1.deleteCurrentRow();
        assertFalse(cursor1.isBeforeFirst());
        assertFalse(cursor1.isAfterLast());
        assertFalse(cursor1.moveToNextRow());
        assertFalse(cursor1.isBeforeFirst());
        assertTrue(cursor1.isAfterLast());

        cursor1.beforeFirst();
        while (cursor1.moveToNextRow()) {
            cursor1.deleteCurrentRow();
        }

        assertTrue(cursor1.isAfterLast());
        assertTrue(cursor2.isCurrentRowDeleted());
        assertTrue(cursor3.isCurrentRowDeleted());
        assertTrue(cursor4.isCurrentRowDeleted());
    }

    @Test
    void testSimpleIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                assertTable(createUnorderedTestTableData(), table);

                Cursor cursor = CursorBuilder.createCursor(idx);
                doTestSimple(cursor, null);
            }
        }
    }

    @Test
    void testMoveIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);
                Cursor cursor = CursorBuilder.createCursor(idx);
                doTestMove(cursor, null);
            }
        }
    }

    @Test
    void testReverseIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);
                Cursor cursor = CursorBuilder.createCursor(idx);
                doTestReverse(cursor, null);
            }
        }
    }

    @Test
    void testSearchIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);
                Cursor cursor = CursorBuilder.createCursor(idx);
                doTestSearch(table, cursor, idx, 42, -13);
            }
        }
    }

    @Test
    void testLiveAdditionIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor1 = CursorBuilder.createCursor(idx);
                Cursor cursor2 = CursorBuilder.createCursor(idx);
                doTestLiveAddition(table, cursor1, cursor2, 11);
            }
        }
    }

    @Test
    void testLiveDeletionIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor1 = CursorBuilder.createCursor(idx);
                Cursor cursor2 = CursorBuilder.createCursor(idx);
                Cursor cursor3 = CursorBuilder.createCursor(idx);
                Cursor cursor4 = CursorBuilder.createCursor(idx);
                doTestLiveDeletion(cursor1, cursor2, cursor3, cursor4, 1);
            }
        }
    }

    @Test
    void testSimpleIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                    List<Map<String, Object>> expectedRows =
                        createTestTableData(3, 9);

                    doTestSimple(cursor, expectedRows);
                }
            }
        }
    }

    @Test
    void testMoveIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                    List<Map<String, Object>> expectedRows =
                        createTestTableData(3, 9);

                    doTestMove(cursor, expectedRows);
                }
            }
        }
    }

    @Test
    void testSearchIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                    doTestSearch(table, cursor, idx, 2, 9);
                }
            }
        }
    }

    @Test
    void testReverseIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                    List<Map<String, Object>> expectedRows =
                        createTestTableData(3, 9);

                    doTestReverse(cursor, expectedRows);
                }
            }
        }
    }

    @Test
    void testLiveAdditionIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor1 = createIndexSubRangeCursor(table, idx, i);
                    Cursor cursor2 = createIndexSubRangeCursor(table, idx, i);

                    doTestLiveAddition(table, cursor1, cursor2, 8);
                }
            }
        }
    }

    @Test
    void testLiveDeletionIndexSubRange() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            for (int i = 0; i < 2; i++) {
                try (Database db = createTestIndexTable(indexCursorDB)) {
                    Table table = db.getTable("test");
                    Index idx = table.getIndexes().get(0);

                    Cursor cursor1 = createIndexSubRangeCursor(table, idx, i);
                    Cursor cursor2 = createIndexSubRangeCursor(table, idx, i);
                    Cursor cursor3 = createIndexSubRangeCursor(table, idx, i);
                    Cursor cursor4 = createIndexSubRangeCursor(table, idx, i);

                    doTestLiveDeletion(cursor1, cursor2, cursor3, cursor4, 4);
                }
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testFindAllIndex(FileFormat fileFormat) throws Exception {
        try (Database db = createDupeTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);

            doTestFindAll(table, cursor, null);
        }
    }

    @Test
    void testFindAll() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createDupeTestTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);
                Cursor cursor = CursorBuilder.createCursor(idx);

                doTestFindAll(table, cursor, idx);
            }
        }
    }

    private static void doTestFindAll(Table table, Cursor cursor, Index index) {
        List<? extends Map<String, Object>> rows = toList(
            cursor.newIterable().withMatchPattern("value", "data2"));

        List<? extends Map<String, Object>> expectedRows = null;

        if (index == null) {
            expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 2, "value", "data2"),
                    createExpectedRow(
                        "id", 5, "value", "data2"),
                    createExpectedRow(
                        "id", 8, "value", "data2"),
                    createExpectedRow(
                        "id", 7, "value", "data2"),
                    createExpectedRow(
                        "id", 2, "value", "data2"));
        } else {
            expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 2, "value", "data2"),
                    createExpectedRow(
                        "id", 2, "value", "data2"),
                    createExpectedRow(
                        "id", 5, "value", "data2"),
                    createExpectedRow(
                        "id", 7, "value", "data2"),
                    createExpectedRow(
                        "id", 8, "value", "data2"));
        }
        assertEquals(expectedRows, rows);

        Column valCol = table.getColumn("value");
        rows = toList(
            cursor.newIterable().withMatchPattern(valCol, "data4"));

        if (index == null) {
            expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 9, "value", "data4"),
                    createExpectedRow(
                        "id", 4, "value", "data4"));
        } else {
            expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 4, "value", "data4"),
                    createExpectedRow(
                        "id", 9, "value", "data4"));
        }
        assertEquals(expectedRows, rows);

        rows = toList(
            cursor.newIterable().withMatchPattern(valCol, "data9"));

        assertTrue(rows.isEmpty());

        rows = toList(
            cursor.newIterable().withMatchPattern(
                Collections.singletonMap("id", 8)));

        expectedRows =
            createExpectedTable(
                createExpectedRow(
                    "id", 8, "value", "data2"),
                createExpectedRow(
                    "id", 8, "value", "data3"));
        assertEquals(expectedRows, rows);

        for (Map<String, Object> row : table) {

            List<Map<String, Object>> tmpRows = new ArrayList<>();
            for (Map<String, Object> tmpRow : cursor) {
                if (row.equals(tmpRow)) {
                    tmpRows.add(tmpRow);
                }
            }
            expectedRows = tmpRows;
            assertFalse(expectedRows.isEmpty());

            rows = toList(cursor.newIterable().withMatchPattern(row));

            assertEquals(expectedRows, rows);
        }

        rows = toList(
            cursor.newIterable().addMatchPattern("id", 8)
                .addMatchPattern("value", "data13"));
        assertTrue(rows.isEmpty());
    }

    @Test
    void testId() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            Database db = createTestIndexTable(indexCursorDB);

            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            Cursor tCursor = CursorBuilder.createCursor(table);
            Cursor iCursor = CursorBuilder.createCursor(idx);

            Cursor.Savepoint tSave = tCursor.getSavepoint();
            Cursor.Savepoint iSave = iCursor.getSavepoint();

            tCursor.restoreSavepoint(tSave);
            iCursor.restoreSavepoint(iSave);

            try {
                tCursor.restoreSavepoint(iSave);
                fail("IllegalArgumentException should have been thrown");
            } catch (IllegalArgumentException e) {
                // success
            }

            try {
                iCursor.restoreSavepoint(tSave);
                fail("IllegalArgumentException should have been thrown");
            } catch (IllegalArgumentException e) {
                // success
            }

            Cursor tCursor2 = CursorBuilder.createCursor(table);
            Cursor iCursor2 = CursorBuilder.createCursor(idx);

            tCursor2.restoreSavepoint(tSave);
            iCursor2.restoreSavepoint(iSave);

            db.close();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testColumnMatcher(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");

            doTestMatchers(table, SimpleColumnMatcher.INSTANCE, false);
            doTestMatchers(table, CaseInsensitiveColumnMatcher.INSTANCE, true);

            Cursor cursor = CursorBuilder.createCursor(table);
            doTestMatcher(table, cursor, SimpleColumnMatcher.INSTANCE, false);
            doTestMatcher(table, cursor, CaseInsensitiveColumnMatcher.INSTANCE, true);
        }
    }

    private static void doTestMatchers(Table table, ColumnMatcher columnMatcher, boolean caseInsensitive) {
        assertTrue(columnMatcher.matches(table, "value", null, null));
        assertFalse(columnMatcher.matches(table, "value", "foo", null));
        assertFalse(columnMatcher.matches(table, "value", null, "foo"));
        assertTrue(columnMatcher.matches(table, "value", "foo", "foo"));
        assertEquals(columnMatcher.matches(table, "value", "foo", "Foo"), caseInsensitive);

        assertFalse(columnMatcher.matches(table, "value", 13, null));
        assertFalse(columnMatcher.matches(table, "value", null, 13));
        assertTrue(columnMatcher.matches(table, "value", 13, 13));
    }

    private static void doTestMatcher(Table table, Cursor cursor, ColumnMatcher columnMatcher, boolean caseInsensitive) throws Exception {
        cursor.setColumnMatcher(columnMatcher);

        assertTrue(cursor.findFirstRow(table.getColumn("id"), 3));
        assertEquals(createExpectedRow("id", 3,
            "value", "data" + 3),
            cursor.getCurrentRow());

        assertTrue(cursor.findFirstRow(createExpectedRow(
            "id", 6,
            "value", "data" + 6)));
        assertEquals(createExpectedRow("id", 6,
            "value", "data" + 6),
            cursor.getCurrentRow());

        assertEquals(cursor.findFirstRow(createExpectedRow(
            "id", 6,
            "value", "Data" + 6)), caseInsensitive);
        if (caseInsensitive) {
            assertEquals(createExpectedRow("id", 6,
                "value", "data" + 6),
                cursor.getCurrentRow());
        }

        assertFalse(cursor.findFirstRow(createExpectedRow(
            "id", 8,
            "value", "data" + 13)));
        assertFalse(cursor.findFirstRow(table.getColumn("id"), 13));
        assertEquals(createExpectedRow("id", 6,
            "value", "data" + 6),
            cursor.getCurrentRow());

        assertTrue(cursor.findFirstRow(createExpectedRow(
            "value", "data" + 7)));
        assertEquals(createExpectedRow("id", 7,
            "value", "data" + 7),
            cursor.getCurrentRow());

        assertEquals(cursor.findFirstRow(createExpectedRow(
            "value", "Data" + 7)), caseInsensitive);
        if (caseInsensitive) {
            assertEquals(createExpectedRow("id", 7,
                "value", "data" + 7),
                cursor.getCurrentRow());
        }

        assertTrue(cursor.findFirstRow(table.getColumn("value"), "data" + 4));
        assertEquals(createExpectedRow("id", 4,
            "value", "data" + 4),
            cursor.getCurrentRow());

        assertEquals(cursor.findFirstRow(table.getColumn("value"), "Data" + 4), caseInsensitive);
        if (caseInsensitive) {
            assertEquals(createExpectedRow("id", 4,
                "value", "data" + 4),
                cursor.getCurrentRow());
        }

        assertEquals(List.of(createExpectedRow("id", 4,
            "value", "data" + 4)),
            toList(
                cursor.newIterable()
                    .withMatchPattern("value", "data4")
                    .withColumnMatcher(SimpleColumnMatcher.INSTANCE)));

        assertEquals(List.of(createExpectedRow("id", 3,
            "value", "data" + 3)),
            toList(
                cursor.newIterable()
                    .withMatchPattern("value", "DaTa3")
                    .withColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE)));

        assertEquals(List.of(createExpectedRow("id", 2,
            "value", "data" + 2)),
            toList(
                cursor.newIterable()
                    .addMatchPattern("value", "DaTa2")
                    .addMatchPattern("id", 2)
                    .withColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE)));
    }

    @Test
    void testIndexCursor() throws Exception {
        for (TestDB testDB : TestDB.getSupportedTestDbsForRead(Basename.INDEX)) {

            try (Database db = testDB.openMem()) {
                Table t1 = db.getTable("Table1");
                Index idx = t1.getIndex(IndexBuilder.PRIMARY_KEY_NAME);
                IndexCursor cursor = CursorBuilder.createCursor(idx);

                assertFalse(cursor.findFirstRowByEntry(-1));
                cursor.findClosestRowByEntry(-1);
                assertEquals(0, cursor.getCurrentRow().get("id"));

                assertTrue(cursor.findFirstRowByEntry(1));
                assertEquals(1, cursor.getCurrentRow().get("id"));

                cursor.findClosestRowByEntry(2);
                assertEquals(2, cursor.getCurrentRow().get("id"));

                assertFalse(cursor.findFirstRowByEntry(4));
                cursor.findClosestRowByEntry(4);
                assertTrue(cursor.isAfterLast());
            }
        }
    }

    @Test
    void testIndexCursorDelete() throws Exception {
        for (TestDB testDB : TestDB.getSupportedTestDbs(Basename.INDEX)) {

            Database db = testDB.openMem();
            Table t1 = db.getTable("Table1");
            Index idx = t1.getIndex("Table2Table1");
            IndexCursor cursor = CursorBuilder.createCursor(idx);

            List<String> expectedData = cursor.newEntryIterable(1)
                .addColumnNames("data")
                .stream().map(r -> r.getString("data"))
                .collect(Collectors.toList());

            assertEquals(List.of("baz11", "baz11-2"), expectedData);

            expectedData = new ArrayList<>();
            for (Iterator<? extends Row> iter =
                cursor.newEntryIterable(1).iterator(); iter.hasNext();) {
                expectedData.add(iter.next().getString("data"));
                iter.remove();
                try {
                    iter.remove();
                    fail("IllegalArgumentException should have been thrown");
                } catch (IllegalStateException e) {
                    // success
                }

                if (!iter.hasNext()) {
                    try {
                        iter.next();
                        fail("NoSuchElementException should have been thrown");
                    } catch (NoSuchElementException e) {
                        // success
                    }
                }
            }

            assertEquals(List.of("baz11", "baz11-2"), expectedData);

            expectedData = new ArrayList<>();
            for (Row row : cursor.newEntryIterable(1)
                .addColumnNames("data")) {
                expectedData.add(row.getString("data"));
            }

            assertTrue(expectedData.isEmpty());

            db.close();
        }
    }

    @Test
    void testCursorDelete() throws Exception {
        for (TestDB testDB : TestDB.getSupportedTestDbs(Basename.INDEX)) {

            try (Database db = testDB.openMem()) {
                Table t1 = db.getTable("Table1");
                Cursor cursor = CursorBuilder.createCursor(t1);

                List<String> expectedData = cursor.newIterable().withColumnNames(
                    List.of("otherfk1", "data")).stream()
                    .filter(r -> r.get("otherfk1").equals(1))
                    .map(r -> r.getString("data"))
                    .collect(Collectors.toList());

                assertEquals(List.of("baz11", "baz11-2"), expectedData);

                expectedData = new ArrayList<>();
                for (Iterator<? extends Row> iter = cursor.iterator(); iter.hasNext();) {
                    Row row = iter.next();
                    if (row.get("otherfk1").equals(1)) {
                        expectedData.add(row.getString("data"));
                        iter.remove();
                        try {
                            iter.remove();
                            fail("IllegalArgumentException should have been thrown");
                        } catch (IllegalStateException e) {
                            // success
                        }
                    }

                    if (!iter.hasNext()) {
                        try {
                            iter.next();
                            fail("NoSuchElementException should have been thrown");
                        } catch (NoSuchElementException e) {
                            // success
                        }
                    }
                }

                assertEquals(List.of("baz11", "baz11-2"), expectedData);

                expectedData = new ArrayList<>();
                for (Row row : cursor.newIterable().withColumnNames(
                    List.of("otherfk1", "data"))) {
                    if (row.get("otherfk1").equals(1)) {
                        expectedData.add(row.getString("data"));
                    }
                }

                assertTrue(expectedData.isEmpty());
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testFindByRowId(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestFindByRowId(cursor);
        }
    }

    @Test
    void testFindByRowIdIndex() throws Exception {
        for (TestDB indexCursorDB : INDEX_CURSOR_DBS) {
            try (Database db = createTestIndexTable(indexCursorDB)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                assertTable(createUnorderedTestTableData(), table);

                Cursor cursor = CursorBuilder.createCursor(idx);
                doTestFindByRowId(cursor);
            }
        }
    }

    private static void doTestFindByRowId(Cursor cursor) throws Exception {
        for (int i = 0; i < 3; i++) {
            cursor.moveToNextRow();
        }

        Row r1 = cursor.getCurrentRow();

        for (int i = 0; i < 3; i++) {
            cursor.moveToNextRow();
        }

        Row r2 = cursor.getCurrentRow();

        doTestFindByRowId(cursor, r1, 2);

        doTestFindByRowId(cursor, r2, 5);
    }

    private static void doTestFindByRowId(Cursor cursor, Row row, int id) throws Exception {
        cursor.reset();
        assertTrue(cursor.findRow(row.getId()));
        Row rFound = cursor.getCurrentRow();
        assertEquals(id, rFound.get("id"));
        assertEquals(row, rFound);
        Cursor.Savepoint save = cursor.getSavepoint();

        assertTrue(cursor.moveToNextRow());
        assertEquals(id + 1, cursor.getCurrentRow().get("id"));

        cursor.restoreSavepoint(save);

        assertTrue(cursor.moveToPreviousRow());
        assertEquals(id - 1, cursor.getCurrentRow().get("id"));

        assertFalse(cursor.findRow(RowIdImpl.FIRST_ROW_ID));

        assertEquals(id - 1, cursor.getCurrentRow().get("id"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testIterationEarlyExit(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            Table table = DatabaseBuilder.newTable("test")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("memo", DataType.MEMO))
                .addIndex(DatabaseBuilder.newIndex("value_idx")
                    .withColumns("value"))
                .toTable(db);

            for (int i = 0; i < 20; i++) {
                Object memo = "memo-" + i;
                table.addRow(i, "val-" + i / 2, memo);
            }

            // generate an "invalid" memo
            byte[] b = new byte[12];
            b[3] = (byte) 0xC0;
            table.addRow(20, "val-9", ColumnImpl.rawDataWrapper(b));

            IndexCursor cursor = CursorBuilder.createCursor(table.getIndex("value_idx"));

            try {
                cursor.newIterable()
                    .addMatchPattern("value", "val-9")
                    .addMatchPattern("memo", "anything")
                    .iterator().hasNext();
                fail("UncheckedIOException should have been thrown");
            } catch (UncheckedIOException ignored) {
                // success
            }

            List<Row> rows = new ArrayList<>();
            for (Row row : cursor.newIterable()
                .addMatchPattern("value", "val-5")
                .addMatchPattern("memo", "memo-11")) {
                rows.add(row);
            }

            assertEquals(rows, createExpectedTable(
                createExpectedRow("id", 11,
                    "value", "val-5",
                    "memo", "memo-11")));

            assertFalse(cursor.newIterable()
                .addMatchPattern("value", "val-31")
                .addMatchPattern("memo", "anything")
                .iterator().hasNext());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testPartialIndexFind(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            TableImpl t = (TableImpl) DatabaseBuilder.newTable("Test").addColumn(DatabaseBuilder.newColumn("id", DataType.LONG)).addColumn(DatabaseBuilder.newColumn("data1", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("num2", DataType.LONG)).addColumn(DatabaseBuilder.newColumn("key3", DataType.TEXT)).addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
                .addIndex(DatabaseBuilder.newIndex("idx3").withColumns("data1", "num2", "key3")).toTable(db);

            Index idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx3", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx3", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx3", idx.getName());

            assertNull(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH));
            assertNull(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH));
            assertNull(t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.EXACT_MATCH));

            DatabaseBuilder.newIndex("idx2").withColumns("data1", "num2").addToTable(t);

            idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx2", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx2", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx3", idx.getName());

            assertNull(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH));
            assertNull(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH));
            assertNull(t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.EXACT_MATCH));

            DatabaseBuilder.newIndex("idx1").withColumns("data1").addToTable(t);

            idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx1", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx2", idx.getName());

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertEquals("idx3", idx.getName());

            assertNull(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH));
            assertNull(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedFileformats")
    void testPartialIndexLookup(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            TableImpl t = (TableImpl) DatabaseBuilder.newTable("Test")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("data1", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("num2", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("key3", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
                .addIndex(DatabaseBuilder.newIndex("idx3")
                    .withColumns(true, "data1")
                    .withColumns(false, "num2")
                    .withColumns(true, "key3"))
                .toTable(db);

            int id = 1;
            for (String str : List.of("A", "B", "C", "D")) {
                for (int i = 4; i >= 0; --i) {
                    // for(int i = 0; i < 5; i++) {
                    for (int j = 1; j < 3; j++) {
                        t.addRow(id, str, i, "K" + j, "value" + id);
                        id++;
                    }
                }
            }

            Index idx = t.getIndex("idx3");
            doPartialIndexLookup(idx);

            idx = DatabaseBuilder.newIndex("idx2")
                .withColumns(true, "data1")
                .withColumns(false, "num2")
                .addToTable(t);
            doPartialIndexLookup(idx);

            idx = DatabaseBuilder.newIndex("idx1")
                .withColumns(true, "data1")
                .addToTable(t);
            doPartialIndexLookup(idx);
        }
    }

    private static void doPartialIndexLookup(Index idx) throws Exception {
        int colCount = idx.getColumnCount();
        IndexCursor c = idx.newCursor().toIndexCursor();

        doFindFirstByEntry(c, 21, "C");
        doFindFirstByEntry(c, null, "Z");

        if (colCount > 1) {
            doFindFirstByEntry(c, 23, "C", 3);
            doFindFirstByEntry(c, null, "C", 20);
        }

        if (colCount > 2) {
            doFindFirstByEntry(c, 27, "C", 1, "K1");
            doFindFirstByEntry(c, null, "C", 4, "K3");
        }

        try {
            if (colCount > 2) {
                c.findFirstRowByEntry("C", 4, "K1", 14);
            } else if (colCount > 1) {
                c.findFirstRowByEntry("C", 4, "K1");
            } else {
                c.findFirstRowByEntry("C", 4);
            }
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
            // scucess
        }

        doFindByEntryRange(c, 11, 20, "B");
        doFindByEntry(c, new int[] {}, "Z");

        if (colCount > 1) {
            doFindByEntryRange(c, 13, 14, "B", 3);
            doFindByEntry(c, new int[] {}, "B", 20);
        }

        if (colCount > 2) {
            doFindByEntryRange(c, 14, 14, "B", 3, "K2");
            doFindByEntry(c, new int[] {}, "B", 3, "K3");
        }

        doFindByRow(idx, 13,
            "data1", "B", "value", "value13");
        doFindByRow(idx, 13,
            "data1", "B", "key3", "K1", "value", "value13");
        doFindByRow(idx, 13,
            "data1", "B", "num2", 3, "key3", "K1", "value", "value13");
        doFindByRow(idx, 13,
            "num2", 3, "value", "value13");
        doFindByRow(idx, 13,
            "value", "value13");
        doFindByRow(idx, null,
            "data1", "B", "num2", 5, "key3", "K1", "value", "value13");
        doFindByRow(idx, null,
            "data1", "B", "value", "value4");

        Column col = idx.getTable().getColumn("data1");
        doFindValue(idx, 21, col, "C");
        doFindValue(idx, null, col, "Z");
        col = idx.getTable().getColumn("value");
        doFindValue(idx, 21, col, "value21");
        doFindValue(idx, null, col, "valueZ");
    }

    private static void doFindFirstByEntry(IndexCursor c, Integer expectedId,
        Object... entry) throws Exception {
        if (expectedId != null) {
            assertTrue(c.findFirstRowByEntry(entry));
            assertEquals(expectedId, c.getCurrentRow().get("id"));
        } else {
            assertFalse(c.findFirstRowByEntry(entry));
        }
    }

    private static void doFindByEntryRange(IndexCursor c, int start, int end,
        Object... entry) {
        List<Integer> expectedIds = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            expectedIds.add(i);
        }
        doFindByEntry(c, expectedIds, entry);
    }

    private static void doFindByEntry(IndexCursor c, int[] ids,
        Object... entry) {
        List<Integer> expectedIds = new ArrayList<>();
        for (int id : ids) {
            expectedIds.add(id);
        }
        doFindByEntry(c, expectedIds, entry);
    }

    private static void doFindByEntry(IndexCursor c, List<Integer> expectedIds,
        Object... entry) {
        List<Integer> foundIds = new ArrayList<>();
        for (Row row : c.newEntryIterable(entry)) {
            foundIds.add((Integer) row.get("id"));
        }
        assertEquals(expectedIds, foundIds);
    }

    private static void doFindByRow(Index idx, Integer id, Object... rowPairs) throws Exception {
        Map<String, Object> map = createExpectedRow(
            rowPairs);
        Row r = CursorBuilder.findRow(idx, map);
        if (id != null) {
            assertEquals(id, r.get("id"));
        } else {
            assertNull(r);
        }
    }

    private static void doFindValue(Index idx, Integer id,
        Column columnPattern, Object valuePattern) throws Exception {
        Object value = CursorBuilder.findValue(
            idx, idx.getTable().getColumn("id"), columnPattern, valuePattern);
        if (id != null) {
            assertEquals(id, value);
        } else {
            assertNull(value);
        }
    }
}
