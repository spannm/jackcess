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

import static io.github.spannm.jackcess.test.Basename.INDEX;
import static io.github.spannm.jackcess.test.Basename.INDEX_CURSOR;
import static io.github.spannm.jackcess.test.TestUtil.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.RowIdImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import io.github.spannm.jackcess.util.CaseInsensitiveColumnMatcher;
import io.github.spannm.jackcess.util.ColumnMatcher;
import io.github.spannm.jackcess.util.SimpleColumnMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

class CursorTest extends AbstractBaseTest {

    @BeforeEach
    void setUp() {
        setTestAutoSync(false);
    }

    @AfterEach
    void tearDown() {
        clearTestAutoSync();
    }

    private static List<Map<String, Object>> createTestTableData() {
        List<Map<String, Object>> expectedRows = new ArrayList<>();
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

    private Database createTestTable(FileFormat fileFormat) throws IOException {
        Database db = createDbMem(fileFormat);

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
        List<Map<String, Object>> expectedRows = new ArrayList<>();
        int[] ids = {3, 7, 6, 1, 2, 9, 0, 5, 4, 8};
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i));
        }
        return expectedRows;
    }

    static Database createTestIndexTable(TestDb _testDb) throws IOException {
        Database db = _testDb.openMem();

        Table table = db.getTable("test");

        for (Map<String, Object> row : createUnorderedTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    private static List<Map<String, Object>> createDupeTestTableData() {
        List<Map<String, Object>> expectedRows =
            new ArrayList<>();
        int[] ids = {3, 7, 6, 1, 2, 9, 0, 5, 4, 8};
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i % 3));
        }
        for (int i : ids) {
            expectedRows.add(createExpectedRow("id", i, "value", "data" + i % 5));
        }
        return expectedRows;
    }

    private Database createDupTestTable(FileFormat _fileFormat) throws IOException {
        Database db = createDbMem(_fileFormat);

        Table table = DatabaseBuilder.newTable("test")
            .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
            .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
            .toTable(db);

        for (Map<String, Object> row : createDupeTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    static Database createDupeTestTable(TestDb _testDb) throws IOException {
        Database db = _testDb.openMem();

        Table table = db.getTable("test");

        for (Map<String, Object> row : createDupeTestTableData()) {
            table.addRow(row.get("id"), row.get("value"));
        }

        return db;
    }

    private static Cursor createIndexSubRangeCursor(Table table,
        Index idx,
        int type) throws IOException {
        return table.newCursor()
            .withIndex(idx)
            .withStartEntry(3 - type)
            .withStartRowInclusive(type == 0)
            .withEndEntry(8 + type)
            .withEndRowInclusive(type == 0)
            .toCursor();
    }

    @Test
    void rowId() {
        // test special cases
        RowIdImpl rowId1 = new RowIdImpl(1, 2);
        RowIdImpl rowId2 = new RowIdImpl(1, 3);
        RowIdImpl rowId3 = new RowIdImpl(2, 1);

        List<RowIdImpl> sortedRowIds =
            new ArrayList<>(new TreeSet<>(
                List.of(rowId1, rowId2, rowId3, RowIdImpl.FIRST_ROW_ID, RowIdImpl.LAST_ROW_ID)));

        assertThat(sortedRowIds).isEqualTo(List.of(RowIdImpl.FIRST_ROW_ID, rowId1, rowId2, rowId3, RowIdImpl.LAST_ROW_ID));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void simple(FileFormat fileFormat) throws Exception {
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
        assertThat(foundRows).isEqualTo(expectedRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void move(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestMove(cursor, null);
        }
    }

    private static void doTestMove(Cursor cursor,
        List<Map<String, Object>> expectedRows) throws IOException {
        if (expectedRows == null) {
            expectedRows = createTestTableData();
        }
        expectedRows.subList(1, 4).clear();

        List<Map<String, Object>> foundRows =
            new ArrayList<>();
        assertThat(cursor.isBeforeFirst()).isTrue();
        assertThat(cursor.isAfterLast()).isFalse();
        foundRows.add(cursor.getNextRow());
        assertThat(cursor.moveNextRows(3)).isEqualTo(3);
        assertThat(cursor.isBeforeFirst()).isFalse();
        assertThat(cursor.isAfterLast()).isFalse();

        Map<String, Object> expectedRow = cursor.getCurrentRow();
        Cursor.Savepoint savepoint = cursor.getSavepoint();
        assertThat(cursor.movePreviousRows(2)).isEqualTo(2);
        assertThat(cursor.moveNextRows(2)).isEqualTo(2);
        assertThat(cursor.moveToNextRow()).isTrue();
        assertThat(cursor.moveToPreviousRow()).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(expectedRow);

        while (cursor.moveToNextRow()) {
            foundRows.add(cursor.getCurrentRow());
        }
        assertThat(foundRows).isEqualTo(expectedRows);
        assertThat(cursor.isBeforeFirst()).isFalse();
        assertThat(cursor.isAfterLast()).isTrue();

        assertThat(cursor.moveNextRows(3)).isEqualTo(0);

        cursor.beforeFirst();
        assertThat(cursor.isBeforeFirst()).isTrue();
        assertThat(cursor.isAfterLast()).isFalse();

        cursor.afterLast();
        assertThat(cursor.isBeforeFirst()).isFalse();
        assertThat(cursor.isAfterLast()).isTrue();

        cursor.restoreSavepoint(savepoint);
        assertThat(cursor.getCurrentRow()).isEqualTo(expectedRow);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void moveNoReset(FileFormat fileFormat) throws Exception {
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
        assertThat(row).isEqualTo(expectedRows.get(4));

        iter = cursor.newIterable().reset(false).iterator();
        iter.next();
        row = iter.next();
        assertThat(row).isEqualTo(expectedRows.get(5));
        iter.next();

        iter = cursor.newIterable().reset(false).iterator();
        for (int i = 6; i < 10; i++) {
            foundRows.add(iter.next());
        }

        assertThat(foundRows).isEqualTo(expectedRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void search(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestSearch(table, cursor, null, 42, -13);
        }
    }

    private static void doTestSearch(Table table, Cursor cursor, Index index, Integer... outOfRangeValues) throws IOException {
        assertThat(cursor.findFirstRow(table.getColumn("id"), 3)).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 3,
                "value", "data" + 3));

        assertThat(cursor.findFirstRow(createExpectedRow(
                "id", 6,
                "value", "data" + 6))).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 6,
                "value", "data" + 6));

        assertThat(cursor.findFirstRow(createExpectedRow(
                "id", 8,
                "value", "data" + 13))).isFalse();
        assertThat(cursor.findFirstRow(table.getColumn("id"), 13)).isFalse();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 6,
                "value", "data" + 6));

        assertThat(cursor.findFirstRow(createExpectedRow(
                "value", "data" + 7))).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 7,
                "value", "data" + 7));

        assertThat(cursor.findFirstRow(table.getColumn("value"), "data" + 4)).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 4,
                "value", "data" + 4));

        for (Integer outOfRangeValue : outOfRangeValues) {
            assertThat(cursor.findFirstRow(table.getColumn("id"),
                    outOfRangeValue)).isFalse();
            assertThat(cursor.findFirstRow(table.getColumn("value"),
                    "data" + outOfRangeValue)).isFalse();
            assertThat(cursor.findFirstRow(createExpectedRow(
                    "id", outOfRangeValue,
                    "value", "data" + outOfRangeValue))).isFalse();
        }

        assertThat(CursorBuilder.findValue(table,
                table.getColumn("value"),
                table.getColumn("id"), 5)).isEqualTo("data" + 5);
        assertThat(CursorBuilder.findRow(table,
                createExpectedRow("id", 5))).isEqualTo(createExpectedRow("id", 5,
                "value", "data" + 5));
        if (index != null) {
            assertThat(CursorBuilder.findValue(index,
                    table.getColumn("value"),
                    table.getColumn("id"), 5)).isEqualTo("data" + 5);
            assertThat(CursorBuilder.findRow(index,
                    createExpectedRow("id", 5))).isEqualTo(createExpectedRow("id", 5,
                    "value", "data" + 5));

            assertThat(CursorBuilder.findValue(index,
                    table.getColumn("value"),
                    table.getColumn("id"),
                    -17)).isNull();
            assertThat(CursorBuilder.findRow(index,
                    createExpectedRow("id", 13))).isNull();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void reverse(FileFormat fileFormat) throws Exception {
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
        assertThat(foundRows).isEqualTo(expectedRows);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void liveAddition(FileFormat fileFormat) throws Exception {
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
        Integer newRowNum) throws IOException {
        cursor1.moveNextRows(11);
        cursor2.moveNextRows(11);

        assertThat(cursor1.isAfterLast()).isTrue();
        assertThat(cursor2.isAfterLast()).isTrue();

        table.addRow(newRowNum, "data" + newRowNum);
        Map<String, Object> expectedRow =
            createExpectedRow("id", newRowNum, "value", "data" + newRowNum);

        assertThat(cursor1.isAfterLast()).isFalse();
        assertThat(cursor2.isAfterLast()).isFalse();

        assertThat(cursor1.getCurrentRow()).isEqualTo(expectedRow);
        assertThat(cursor2.getCurrentRow()).isEqualTo(expectedRow);
        assertThat(cursor1.moveToNextRow()).isFalse();
        assertThat(cursor2.moveToNextRow()).isFalse();
        assertThat(cursor1.isAfterLast()).isTrue();
        assertThat(cursor2.isAfterLast()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void liveDeletion(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");

            Cursor cursor1 = CursorBuilder.createCursor(table);
            Cursor cursor2 = CursorBuilder.createCursor(table);
            Cursor cursor3 = CursorBuilder.createCursor(table);
            Cursor cursor4 = CursorBuilder.createCursor(table);
            doTestLiveDeletion(cursor1, cursor2, cursor3, cursor4, 1);
        }
    }

    private static void doTestLiveDeletion(Cursor cursor1, Cursor cursor2, Cursor cursor3, Cursor cursor4, int firstValue) throws IOException {
        assertThat(cursor1.moveNextRows(2)).isEqualTo(2);
        assertThat(cursor2.moveNextRows(3)).isEqualTo(3);
        assertThat(cursor3.moveNextRows(3)).isEqualTo(3);
        assertThat(cursor4.moveNextRows(4)).isEqualTo(4);

        Map<String, Object> expectedPrevRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);
        firstValue++;
        Map<String, Object> expectedDeletedRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);
        firstValue++;
        Map<String, Object> expectedNextRow = createExpectedRow("id", firstValue, "value", "data" + firstValue);

        assertThat(cursor2.getCurrentRow()).isEqualTo(expectedDeletedRow);
        assertThat(cursor3.getCurrentRow()).isEqualTo(expectedDeletedRow);

        assertThat(cursor2.isCurrentRowDeleted()).isFalse();
        assertThat(cursor3.isCurrentRowDeleted()).isFalse();

        cursor2.deleteCurrentRow();

        assertThat(cursor2.isCurrentRowDeleted()).isTrue();
        assertThat(cursor3.isCurrentRowDeleted()).isTrue();

        assertThat(cursor1.getNextRow()).isEqualTo(expectedNextRow);
        assertThat(cursor2.getNextRow()).isEqualTo(expectedNextRow);
        assertThat(cursor3.getNextRow()).isEqualTo(expectedNextRow);

        assertThat(cursor3.getPreviousRow()).isEqualTo(expectedPrevRow);

        assertThat(cursor3.moveToNextRow()).isTrue();
        cursor3.deleteCurrentRow();
        assertThat(cursor3.isCurrentRowDeleted()).isTrue();

        firstValue += 2;
        expectedNextRow =
            createExpectedRow("id", firstValue, "value", "data" + firstValue);
        assertThat(cursor3.moveToNextRow()).isTrue();
        assertThat(cursor3.getNextRow()).isEqualTo(expectedNextRow);

        cursor1.beforeFirst();
        assertThat(cursor1.moveToNextRow()).isTrue();
        cursor1.deleteCurrentRow();
        assertThat(cursor1.isBeforeFirst()).isFalse();
        assertThat(cursor1.isAfterLast()).isFalse();
        assertThat(cursor1.moveToPreviousRow()).isFalse();
        assertThat(cursor1.isBeforeFirst()).isTrue();
        assertThat(cursor1.isAfterLast()).isFalse();

        cursor1.afterLast();
        assertThat(cursor1.moveToPreviousRow()).isTrue();
        cursor1.deleteCurrentRow();
        assertThat(cursor1.isBeforeFirst()).isFalse();
        assertThat(cursor1.isAfterLast()).isFalse();
        assertThat(cursor1.moveToNextRow()).isFalse();
        assertThat(cursor1.isBeforeFirst()).isFalse();
        assertThat(cursor1.isAfterLast()).isTrue();

        cursor1.beforeFirst();
        while (cursor1.moveToNextRow()) {
            cursor1.deleteCurrentRow();
        }

        assertThat(cursor1.isAfterLast()).isTrue();
        assertThat(cursor2.isCurrentRowDeleted()).isTrue();
        assertThat(cursor3.isCurrentRowDeleted()).isTrue();
        assertThat(cursor4.isCurrentRowDeleted()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void simpleIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            assertTable(createUnorderedTestTableData(), table);

            Cursor cursor = CursorBuilder.createCursor(idx);
            doTestSimple(cursor, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void moveIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);
            Cursor cursor = CursorBuilder.createCursor(idx);
            doTestMove(cursor, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void reverseIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);
            Cursor cursor = CursorBuilder.createCursor(idx);
            doTestReverse(cursor, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void searchIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);
            Cursor cursor = CursorBuilder.createCursor(idx);
            doTestSearch(table, cursor, idx, 42, -13);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void liveAdditionIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            Cursor cursor1 = CursorBuilder.createCursor(idx);
            Cursor cursor2 = CursorBuilder.createCursor(idx);
            doTestLiveAddition(table, cursor1, cursor2, 11);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void liveDeletionIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            Cursor cursor1 = CursorBuilder.createCursor(idx);
            Cursor cursor2 = CursorBuilder.createCursor(idx);
            Cursor cursor3 = CursorBuilder.createCursor(idx);
            Cursor cursor4 = CursorBuilder.createCursor(idx);
            doTestLiveDeletion(cursor1, cursor2, cursor3, cursor4, 1);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void simpleIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                List<Map<String, Object>> expectedRows =
                    createTestTableData(3, 9);

                doTestSimple(cursor, expectedRows);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void moveIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                List<Map<String, Object>> expectedRows =
                    createTestTableData(3, 9);

                doTestMove(cursor, expectedRows);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void searchIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                doTestSearch(table, cursor, idx, 2, 9);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void reverseIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor = createIndexSubRangeCursor(table, idx, i);

                List<Map<String, Object>> expectedRows =
                    createTestTableData(3, 9);

                doTestReverse(cursor, expectedRows);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void liveAdditionIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
                Table table = db.getTable("test");
                Index idx = table.getIndexes().get(0);

                Cursor cursor1 = createIndexSubRangeCursor(table, idx, i);
                Cursor cursor2 = createIndexSubRangeCursor(table, idx, i);

                doTestLiveAddition(table, cursor1, cursor2, 8);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void liveDeletionIndexSubRange(TestDb testDb) throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Database db = createTestIndexTable(testDb)) {
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

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void findAllIndex(FileFormat fileFormat) throws Exception {
        try (Database testDb = createDupTestTable(fileFormat)) {
            Table table = testDb.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);

            doTestFindAll(table, cursor, null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void findAll(TestDb testDb) throws Exception {
        try (Database db = createDupeTestTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);
            Cursor cursor = CursorBuilder.createCursor(idx);

            doTestFindAll(table, cursor, idx);
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
        assertThat(rows).isEqualTo(expectedRows);

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
        assertThat(rows).isEqualTo(expectedRows);

        rows = toList(
            cursor.newIterable().withMatchPattern(valCol, "data9"));

        assertThat(rows.isEmpty()).isTrue();

        rows = toList(
            cursor.newIterable().withMatchPattern(
                Collections.singletonMap("id", 8)));

        expectedRows =
            createExpectedTable(
                createExpectedRow(
                    "id", 8, "value", "data2"),
                createExpectedRow(
                    "id", 8, "value", "data3"));
        assertThat(rows).isEqualTo(expectedRows);

        for (Map<String, Object> row : table) {

            List<Map<String, Object>> tmpRows = new ArrayList<>();
            for (Map<String, Object> tmpRow : cursor) {
                if (row.equals(tmpRow)) {
                    tmpRows.add(tmpRow);
                }
            }
            expectedRows = tmpRows;
            assertThat(expectedRows.isEmpty()).isFalse();

            rows = toList(cursor.newIterable().withMatchPattern(row));

            assertThat(rows).isEqualTo(expectedRows);
        }

        rows = toList(
            cursor.newIterable().addMatchPattern("id", 8)
                .addMatchPattern("value", "data13"));
        assertThat(rows.isEmpty()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void id(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            Cursor tCursor = CursorBuilder.createCursor(table);
            Cursor iCursor = CursorBuilder.createCursor(idx);

            Cursor.Savepoint tSave = tCursor.getSavepoint();
            Cursor.Savepoint iSave = iCursor.getSavepoint();

            tCursor.restoreSavepoint(tSave);
            iCursor.restoreSavepoint(iSave);

            assertThrows(IllegalArgumentException.class, () -> tCursor.restoreSavepoint(iSave));

            assertThrows(IllegalArgumentException.class, () -> iCursor.restoreSavepoint(tSave));

            Cursor tCursor2 = CursorBuilder.createCursor(table);
            Cursor iCursor2 = CursorBuilder.createCursor(idx);

            tCursor2.restoreSavepoint(tSave);
            iCursor2.restoreSavepoint(iSave);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void columnMatcher(FileFormat fileFormat) throws Exception {
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
        assertThat(columnMatcher.matches(table, "value", null, null)).isTrue();
        assertThat(columnMatcher.matches(table, "value", "foo", null)).isFalse();
        assertThat(columnMatcher.matches(table, "value", null, "foo")).isFalse();
        assertThat(columnMatcher.matches(table, "value", "foo", "foo")).isTrue();
        assertThat(caseInsensitive).isEqualTo(columnMatcher.matches(table, "value", "foo", "Foo"));

        assertThat(columnMatcher.matches(table, "value", 13, null)).isFalse();
        assertThat(columnMatcher.matches(table, "value", null, 13)).isFalse();
        assertThat(columnMatcher.matches(table, "value", 13, 13)).isTrue();
    }

    private static void doTestMatcher(Table table, Cursor cursor, ColumnMatcher columnMatcher, boolean caseInsensitive) throws IOException {
        cursor.setColumnMatcher(columnMatcher);

        assertThat(cursor.findFirstRow(table.getColumn("id"), 3)).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 3,
                "value", "data" + 3));

        assertThat(cursor.findFirstRow(createExpectedRow(
                "id", 6,
                "value", "data" + 6))).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 6,
                "value", "data" + 6));

        assertThat(caseInsensitive).isEqualTo(cursor.findFirstRow(createExpectedRow(
                "id", 6,
                "value", "Data" + 6)));
        if (caseInsensitive) {
            assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 6,
                    "value", "data" + 6));
        }

        assertThat(cursor.findFirstRow(createExpectedRow(
                "id", 8,
                "value", "data" + 13))).isFalse();
        assertThat(cursor.findFirstRow(table.getColumn("id"), 13)).isFalse();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 6,
                "value", "data" + 6));

        assertThat(cursor.findFirstRow(createExpectedRow(
                "value", "data" + 7))).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 7,
                "value", "data" + 7));

        assertThat(caseInsensitive).isEqualTo(cursor.findFirstRow(createExpectedRow(
                "value", "Data" + 7)));
        if (caseInsensitive) {
            assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 7,
                    "value", "data" + 7));
        }

        assertThat(cursor.findFirstRow(table.getColumn("value"), "data" + 4)).isTrue();
        assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 4,
                "value", "data" + 4));

        assertThat(caseInsensitive).isEqualTo(cursor.findFirstRow(table.getColumn("value"), "Data" + 4));
        if (caseInsensitive) {
            assertThat(cursor.getCurrentRow()).isEqualTo(createExpectedRow("id", 4,
                    "value", "data" + 4));
        }

        assertThat(toList(
                cursor.newIterable()
                        .withMatchPattern("value", "data4")
                        .withColumnMatcher(SimpleColumnMatcher.INSTANCE))).isEqualTo(List.of(createExpectedRow("id", 4,
                "value", "data" + 4)));

        assertThat(toList(
                cursor.newIterable()
                        .withMatchPattern("value", "DaTa3")
                        .withColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE))).isEqualTo(List.of(createExpectedRow("id", 3,
                "value", "data" + 3)));

        assertThat(toList(
                cursor.newIterable()
                        .addMatchPattern("value", "DaTa2")
                        .addMatchPattern("id", 2)
                        .withColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE))).isEqualTo(List.of(createExpectedRow("id", 2,
                "value", "data" + 2)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(INDEX)
    void indexCursor(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            Table t1 = db.getTable("Table1");
            Index idx = t1.getIndex(IndexBuilder.PRIMARY_KEY_NAME);
            IndexCursor cursor = CursorBuilder.createCursor(idx);

            assertThat(cursor.findFirstRowByEntry(-1)).isFalse();
            cursor.findClosestRowByEntry(-1);
            assertThat(cursor.getCurrentRow().get("id")).isEqualTo(0);

            assertThat(cursor.findFirstRowByEntry(1)).isTrue();
            assertThat(cursor.getCurrentRow().get("id")).isEqualTo(1);

            cursor.findClosestRowByEntry(2);
            assertThat(cursor.getCurrentRow().get("id")).isEqualTo(2);

            assertThat(cursor.findFirstRowByEntry(4)).isFalse();
            cursor.findClosestRowByEntry(4);
            assertThat(cursor.isAfterLast()).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void indexCursorDelete(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            Table t1 = db.getTable("Table1");
            Index idx = t1.getIndex("Table2Table1");
            IndexCursor cursor = CursorBuilder.createCursor(idx);

            List<String> expectedData = cursor.newEntryIterable(1)
                .addColumnNames("data")
                .stream().map(r -> r.getString("data"))
                .collect(Collectors.toList());

            assertThat(expectedData).isEqualTo(List.of("baz11", "baz11-2"));

            List<String> viaIterableNames = cursor.newEntryIterable(1)
                .addColumnNames(List.of("data"))
                .stream().map(r -> r.getString("data"))
                .collect(Collectors.toList());
            assertThat(viaIterableNames).isEqualTo(List.of("baz11", "baz11-2"));

            List<String> viaColumns = cursor.newEntryIterable(1)
                .addColumns(List.of(t1.getColumn("data")))
                .stream().map(r -> r.getString("data"))
                .collect(Collectors.toList());
            assertThat(viaColumns).isEqualTo(List.of("baz11", "baz11-2"));

            List<String> viaWithEntryValues = cursor.newEntryIterable(2)
                .withEntryValues(1)
                .withColumnMatcher(SimpleColumnMatcher.INSTANCE)
                .addColumnNames("data")
                .stream().map(r -> r.getString("data"))
                .collect(Collectors.toList());
            assertThat(viaWithEntryValues).isEqualTo(List.of("baz11", "baz11-2"));

            expectedData = new ArrayList<>();
            for (Iterator<? extends Row> iter =
                cursor.newEntryIterable(1).iterator(); iter.hasNext();) {
                expectedData.add(iter.next().getString("data"));
                iter.remove();
                assertThrows(IllegalStateException.class, iter::remove);

                if (!iter.hasNext()) {
                    assertThrows(NoSuchElementException.class, iter::next);
                }
            }

            assertThat(expectedData).isEqualTo(List.of("baz11", "baz11-2"));

            expectedData = new ArrayList<>();
            for (Row row : cursor.newEntryIterable(1).addColumnNames("data")) {
                expectedData.add(row.getString("data"));
            }

            assertThat(expectedData.isEmpty()).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX)
    void cursorDelete(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            Table t1 = db.getTable("Table1");
            Cursor cursor = CursorBuilder.createCursor(t1);

            List<String> expectedData = cursor.newIterable().withColumnNames(
                List.of("otherfk1", "data")).stream()
                .filter(r -> r.get("otherfk1").equals(1))
                .map(r -> r.getString("data"))
                .collect(Collectors.toList());

            assertThat(expectedData).isEqualTo(List.of("baz11", "baz11-2"));

            expectedData = new ArrayList<>();
            for (Iterator<? extends Row> iter = cursor.iterator(); iter.hasNext();) {
                Row row = iter.next();
                if (row.get("otherfk1").equals(1)) {
                    expectedData.add(row.getString("data"));
                    iter.remove();
                    assertThrows(IllegalStateException.class, iter::remove);
                }

                if (!iter.hasNext()) {
                    assertThrows(NoSuchElementException.class, iter::next);
                }
            }

            assertThat(expectedData).isEqualTo(List.of("baz11", "baz11-2"));

            expectedData = new ArrayList<>();
            for (Row row : cursor.newIterable().withColumnNames(
                List.of("otherfk1", "data"))) {
                if (row.get("otherfk1").equals(1)) {
                    expectedData.add(row.getString("data"));
                }
            }

            assertThat(expectedData.isEmpty()).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void findByRowId(FileFormat fileFormat) throws Exception {
        try (Database db = createTestTable(fileFormat)) {
            Table table = db.getTable("test");
            Cursor cursor = CursorBuilder.createCursor(table);
            doTestFindByRowId(cursor);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void findByRowIdIndex(TestDb testDb) throws Exception {
        try (Database db = createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            Index idx = table.getIndexes().get(0);

            assertTable(createUnorderedTestTableData(), table);

            Cursor cursor = CursorBuilder.createCursor(idx);
            doTestFindByRowId(cursor);
        }
    }

    private static void doTestFindByRowId(Cursor cursor) throws IOException {
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

    private static void doTestFindByRowId(Cursor cursor, Row row, int id) throws IOException {
        cursor.reset();
        assertThat(cursor.findRow(row.getId())).isTrue();
        Row rFound = cursor.getCurrentRow();
        assertThat(rFound.get("id")).isEqualTo(id);
        assertThat(rFound).isEqualTo(row);
        Cursor.Savepoint save = cursor.getSavepoint();

        assertThat(cursor.moveToNextRow()).isTrue();
        assertThat(cursor.getCurrentRow().get("id")).isEqualTo(id + 1);

        cursor.restoreSavepoint(save);

        assertThat(cursor.moveToPreviousRow()).isTrue();
        assertThat(cursor.getCurrentRow().get("id")).isEqualTo(id - 1);

        assertThat(cursor.findRow(RowIdImpl.FIRST_ROW_ID)).isFalse();

        assertThat(cursor.getCurrentRow().get("id")).isEqualTo(id - 1);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void iterationEarlyExit(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
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

            assertThrows(UncheckedIOException.class, () -> cursor.newIterable()
                .addMatchPattern("value", "val-9")
                .addMatchPattern("memo", "anything")
                .iterator().hasNext());

            List<Row> rows = new ArrayList<>();
            for (Row row : cursor.newIterable()
                .addMatchPattern("value", "val-5")
                .addMatchPattern("memo", "memo-11")) {
                rows.add(row);
            }

            assertThat(createExpectedTable(
                    createExpectedRow("id", 11,
                            "value", "val-5",
                            "memo", "memo-11"))).isEqualTo(rows);

            assertThat(cursor.newIterable()
                    .addMatchPattern("value", "val-31")
                    .addMatchPattern("memo", "anything")
                    .iterator().hasNext()).isFalse();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void partialIndexFind(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            TableImpl t = (TableImpl) DatabaseBuilder.newTable("Test").addColumn(DatabaseBuilder.newColumn("id", DataType.LONG)).addColumn(DatabaseBuilder.newColumn("data1", DataType.TEXT))
                .addColumn(DatabaseBuilder.newColumn("num2", DataType.LONG)).addColumn(DatabaseBuilder.newColumn("key3", DataType.TEXT)).addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
                .addIndex(DatabaseBuilder.newIndex("idx3").withColumns("data1", "num2", "key3")).toTable(db);

            Index idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx3");

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx3");

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx3");

            assertThat(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
            assertThat(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
            assertThat(t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.EXACT_MATCH)).isNull();

            DatabaseBuilder.newIndex("idx2").withColumns("data1", "num2").addToTable(t);

            idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx2");

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx2");

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx3");

            assertThat(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
            assertThat(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
            assertThat(t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.EXACT_MATCH)).isNull();

            DatabaseBuilder.newIndex("idx1").withColumns("data1").addToTable(t);

            idx = t.findIndexForColumns(List.of("data1"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx1");

            idx = t.findIndexForColumns(List.of("data1", "num2"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx2");

            idx = t.findIndexForColumns(List.of("data1", "num2", "key3"), TableImpl.IndexFeature.ANY_MATCH);
            assertThat(idx.getName()).isEqualTo("idx3");

            assertThat(t.findIndexForColumns(List.of("num2"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
            assertThat(t.findIndexForColumns(List.of("data1", "key3"), TableImpl.IndexFeature.ANY_MATCH)).isNull();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void partialIndexLookup(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
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

    private static void doPartialIndexLookup(Index idx) throws IOException {
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

        assertThrows(IllegalArgumentException.class, () -> {
            if (colCount > 2) {
                c.findFirstRowByEntry("C", 4, "K1", 14);
            } else if (colCount > 1) {
                c.findFirstRowByEntry("C", 4, "K1");
            } else {
                c.findFirstRowByEntry("C", 4);
            }
        });

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
        Object... entry) throws IOException {
        if (expectedId != null) {
            assertThat(c.findFirstRowByEntry(entry)).isTrue();
            assertThat(c.getCurrentRow().get("id")).isEqualTo(expectedId);
        } else {
            assertThat(c.findFirstRowByEntry(entry)).isFalse();
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
        assertThat(foundIds).isEqualTo(expectedIds);
    }

    private static void doFindByRow(Index idx, Integer id, Object... rowPairs) throws IOException {
        Map<String, Object> map = createExpectedRow(
            rowPairs);
        Row r = CursorBuilder.findRow(idx, map);
        if (id != null) {
            assertThat(r.get("id")).isEqualTo(id);
        } else {
            assertThat(r).isNull();
        }
    }

    private static void doFindValue(Index idx, Integer id,
        Column columnPattern, Object valuePattern) throws IOException {
        Object value = CursorBuilder.findValue(
            idx, idx.getTable().getColumn("id"), columnPattern, valuePattern);
        if (id != null) {
            assertThat(value).isEqualTo(id);
        } else {
            assertThat(value).isNull();
        }
    }
}
