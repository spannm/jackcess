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

package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.*;

/**
 * @author James Ahlborn
 */
class IndexTest extends AbstractBaseTest {

    @BeforeEach
    void setAutoSyncOff() {
        setTestAutoSync(false);
    }

    @AfterEach
    void clearAutoSync() {
        clearTestAutoSync();
    }

    @Test
    void testByteOrder() {
        byte b1 = (byte) 0x00;
        byte b2 = (byte) 0x01;
        byte b3 = (byte) 0x7F;
        byte b4 = (byte) 0x80;
        byte b5 = (byte) 0xFF;

        assertTrue(ByteUtil.asUnsignedByte(b1) < ByteUtil.asUnsignedByte(b2));
        assertTrue(ByteUtil.asUnsignedByte(b2) < ByteUtil.asUnsignedByte(b3));
        assertTrue(ByteUtil.asUnsignedByte(b3) < ByteUtil.asUnsignedByte(b4));
        assertTrue(ByteUtil.asUnsignedByte(b4) < ByteUtil.asUnsignedByte(b5));
    }

    @Test
    void testByteCodeComparator() {
        byte[] b0 = null;
        byte[] b1 = new byte[] {(byte) 0x00};
        byte[] b2 = new byte[] {(byte) 0x00, (byte) 0x00};
        byte[] b3 = new byte[] {(byte) 0x00, (byte) 0x01};
        byte[] b4 = new byte[] {(byte) 0x01};
        byte[] b5 = new byte[] {(byte) 0x80};
        byte[] b6 = new byte[] {(byte) 0xFF};
        byte[] b7 = new byte[] {(byte) 0xFF, (byte) 0x00};
        byte[] b8 = new byte[] {(byte) 0xFF, (byte) 0x01};

        List<byte[]> expectedList = Arrays.asList(b0, b1, b2, b3, b4, b5, b6, b7, b8);
        SortedSet<byte[]> sortedSet = new TreeSet<>(IndexData.BYTE_CODE_COMPARATOR);
        sortedSet.addAll(expectedList);
        assertEquals(expectedList, new ArrayList<>(sortedSet));

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST, readOnly = true)
    void testPrimaryKey(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Table1");
            Map<String, Boolean> foundPKs = new HashMap<>();
            Index pkIndex = null;
            for (Index index : table.getIndexes()) {
                foundPKs.put(index.getColumns().iterator().next().getName(),
                    index.isPrimaryKey());
                if (index.isPrimaryKey()) {
                    pkIndex = index;

                }
            }
            Map<String, Boolean> expectedPKs = new HashMap<>();
            expectedPKs.put("A", Boolean.TRUE);
            expectedPKs.put("B", Boolean.FALSE);
            assertEquals(expectedPKs, foundPKs);
            assertSame(pkIndex, table.getPrimaryKeyIndex());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.INDEX, readOnly = true)
    void testLogicalIndexes(TestDb testDb) throws Exception {

        try (Database db = testDb.open()) {
            TableImpl table = (TableImpl) db.getTable("Table1");
            for (IndexImpl idx : table.getIndexes()) {
                idx.initialize();
            }
            assertEquals(4, table.getIndexes().size());
            assertEquals(4, table.getLogicalIndexCount());
            checkIndexColumns(table,
                "id", "id",
                "PrimaryKey", "id",
                "Table2Table1", "otherfk1",
                "Table3Table1", "otherfk2");

            table = (TableImpl) db.getTable("Table2");
            for (IndexImpl idx : table.getIndexes()) {
                idx.initialize();
            }
            assertEquals(3, table.getIndexes().size());
            assertEquals(2, table.getIndexDatas().size());
            assertEquals(3, table.getLogicalIndexCount());
            checkIndexColumns(table,
                "id", "id",
                "PrimaryKey", "id",
                ".rC", "id");

            IndexImpl pkIdx = table.getIndex("PrimaryKey");
            IndexImpl fkIdx = table.getIndex(".rC");
            assertNotSame(pkIdx, fkIdx);
            assertTrue(fkIdx.isForeignKey());
            assertSame(pkIdx.getIndexData(), fkIdx.getIndexData());
            IndexData indexData = pkIdx.getIndexData();
            assertEquals(List.of(pkIdx, fkIdx), indexData.getIndexes());
            assertSame(pkIdx, indexData.getPrimaryIndex());

            table = (TableImpl) db.getTable("Table3");
            for (IndexImpl idx : table.getIndexes()) {
                idx.initialize();
            }
            assertEquals(3, table.getIndexes().size());
            assertEquals(2, table.getIndexDatas().size());
            assertEquals(3, table.getLogicalIndexCount());
            checkIndexColumns(table,
                "id", "id",
                "PrimaryKey", "id",
                ".rC", "id");

            pkIdx = table.getIndex("PrimaryKey");
            fkIdx = table.getIndex(".rC");
            assertNotSame(pkIdx, fkIdx);
            assertTrue(fkIdx.isForeignKey());
            assertSame(pkIdx.getIndexData(), fkIdx.getIndexData());
            indexData = pkIdx.getIndexData();
            assertEquals(List.of(pkIdx, fkIdx), indexData.getIndexes());
            assertSame(pkIdx, indexData.getPrimaryIndex());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.COMP_INDEX)
    void testComplexIndex(TestDb testDb) throws Exception {
        try (// this file has an index with "compressed" entries and node pages
        Database db1 = testDb.open()) {
            TableImpl t1 = (TableImpl) db1.getTable("Table1");
            IndexImpl idx1 = t1.getIndexes().get(0);
            assertFalse(idx1.isInitialized());
            assertEquals(512, TestUtil.countRows(t1));
            assertEquals(512, idx1.getIndexData().getEntryCount());
        }

        try (// copy to temp file and attempt to edit
        Database db2 = testDb.openCopy()) {
            TableImpl t2 = (TableImpl) db2.getTable("Table1");
            t2.addRow(99, "abc", "def");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST)
    void testEntryDeletion(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            Table table = db.getTable("Table1");

            for (int i = 0; i < 10; i++) {
                table.addRow("foo" + i, "bar" + i, (byte) 42 + i, (short) 53 + i, 13 * i,
                    6.7d / i, null, null, true);
            }
            table.reset();
            TestUtil.assertRowCount(12, table);

            for (Index index : table.getIndexes()) {
                assertEquals(12, ((IndexImpl) index).getIndexData().getEntryCount());
            }

            table.reset();
            table.getNextRow();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();
            table.getNextRow();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();
            table.getNextRow();
            table.getNextRow();
            table.getNextRow();
            table.getDefaultCursor().deleteCurrentRow();

            table.reset();
            TestUtil.assertRowCount(8, table);

            for (Index index : table.getIndexes()) {
                assertEquals(8, ((IndexImpl) index).getIndexData().getEntryCount());
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.INDEX_PROPERTIES)
    void testIgnoreNulls(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            db.setEvaluateExpressions(false);

            doTestIgnoreNulls(db, "TableIgnoreNulls1");
            doTestIgnoreNulls(db, "TableIgnoreNulls2");
        }
    }

    private void doTestIgnoreNulls(Database db, String tableName) throws Exception {
        Table orig = db.getTable(tableName);
        IndexImpl origI = (IndexImpl) orig.getIndex("DataIndex");
        Table temp = db.getTable(tableName + "_temp");
        IndexImpl tempI = (IndexImpl) temp.getIndex("DataIndex");

        // copy from orig table to temp table
        for (Map<String, Object> row : orig) {
            temp.addRow(orig.asRow(row));
        }

        assertEquals(origI.getIndexData().getEntryCount(),
            tempI.getIndexData().getEntryCount());

        Cursor origC = origI.newCursor().toCursor();
        Cursor tempC = tempI.newCursor().toCursor();

        while (true) {
            boolean origHasNext = origC.moveToNextRow();
            boolean tempHasNext = tempC.moveToNextRow();
            assertEquals(origHasNext, tempHasNext);
            if (!origHasNext) {
                break;
            }

            Map<String, Object> origRow = origC.getCurrentRow();
            Cursor.Position origCurPos = origC.getSavepoint().getCurrentPosition();
            Map<String, Object> tempRow = tempC.getCurrentRow();
            Cursor.Position tempCurPos = tempC.getSavepoint().getCurrentPosition();

            assertEquals(origRow, tempRow);
            assertEquals(IndexCodesTest.entryToString(origCurPos), IndexCodesTest.entryToString(tempCurPos));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.INDEX_PROPERTIES)
    void testUnique(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            Table t = db.getTable("TableUnique1_temp");
            Index index = t.getIndex("DataIndex");

            doTestUnique(index, 1,
                null, true,
                "unique data", true,
                null, true,
                "more", false,
                "stuff", false,
                "unique data", false);

            t = db.getTable("TableUnique2_temp");
            index = t.getIndex("DataIndex");

            doTestUnique(index, 2,
                null, null, true,
                "unique data", 42, true,
                "unique data", null, true,
                null, null, true,
                "some", 42, true,
                "more unique data", 13, true,
                null, -4242, true,
                "another row", -3462, false,
                null, 49, false,
                "more", null, false,
                "unique data", 42, false,
                "unique data", null, false,
                null, -4242, false);
        }
    }

    private void doTestUnique(Index index, int numValues, Object... testData) {
        for (int i = 0; i < testData.length; i += numValues + 1) {
            Object[] row = new Object[numValues + 1];
            row[0] = "testRow" + i;
            if (numValues + 1 - 1 >= 0) {
                System.arraycopy(testData, i + 1 - 1, row, 1, numValues + 1 - 1);
            }
            boolean expectedSuccess = (Boolean) testData[i + numValues];

            IOException failure = null;
            try {
                ((IndexImpl) index).getIndexData().prepareAddRow(
                    row, new RowIdImpl(400 + i, 0), null).commit();
            } catch (IOException e) {
                failure = e;
            }
            if (expectedSuccess) {
                assertNull(failure);
            } else {
                assertNotNull(failure);
                assertTrue(failure.getMessage().contains("uniqueness"));
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST)
    void testUniqueEntryCount(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            db.setDateTimeType(DateTimeType.DATE);
            Table table = db.getTable("Table1");
            IndexImpl indA = (IndexImpl) table.getIndex("PrimaryKey");
            IndexImpl indB = (IndexImpl) table.getIndex("B");

            assertEquals(2, indA.getUniqueEntryCount());
            assertEquals(2, indB.getUniqueEntryCount());

            List<String> bElems = Arrays.asList("bar", null, "baz", "argle", null, "bazzle", "37", "bar", "bar", "BAZ");

            for (int i = 0; i < 10; i++) {
                table.addRow("foo" + i, bElems.get(i), (byte) 42 + i, (short) 53 + i, 13 * i, 6.7d / i, null, null, true);
            }

            assertEquals(12, indA.getIndexData().getEntryCount());
            assertEquals(12, indB.getIndexData().getEntryCount());

            assertEquals(12, indA.getUniqueEntryCount());
            assertEquals(8, indB.getUniqueEntryCount());

            table = null;
            indA = null;
            indB = null;

            table = db.getTable("Table1");
            indA = (IndexImpl) table.getIndex("PrimaryKey");
            indB = (IndexImpl) table.getIndex("B");

            assertEquals(12, indA.getIndexData().getEntryCount());
            assertEquals(12, indB.getIndexData().getEntryCount());

            assertEquals(12, indA.getUniqueEntryCount());
            assertEquals(8, indB.getUniqueEntryCount());

            Cursor c = CursorBuilder.createCursor(table);
            assertTrue(c.moveToNextRow());

            final Row row = c.getCurrentRow();
            // Row order is arbitrary, so v2007 row order difference is valid
            if (testDb.getExpectedFileFormat().ordinal() >= FileFormat.V2007.ordinal()) {
                TestUtil.checkTestDBTable1RowA(testDb, table, row);
            } else {
                TestUtil.checkTestDBTable1RowABCDEFG(testDb, table, row);
            }
            c.deleteCurrentRow();

            assertEquals(11, indA.getIndexData().getEntryCount());
            assertEquals(11, indB.getIndexData().getEntryCount());

            assertEquals(12, indA.getUniqueEntryCount());
            assertEquals(8, indB.getUniqueEntryCount());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST)
    void testReplId(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            Table table = db.getTable("Table4");

            for (int i = 0; i < 20; i++) {
                table.addRow("row" + i, Column.AUTO_NUMBER);
            }

            assertEquals(20, table.getRowCount());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource()
    void testIndexCreation(FileFormat fileFormat) throws Exception {
        try (Database db = TestUtil.create(fileFormat)) {
            Table t = DatabaseBuilder.newTable("TestTable")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("data", DataType.TEXT))
                .withPrimaryKey("id")
                .toTable(db);

            assertEquals(1, t.getIndexes().size());
            IndexImpl idx = (IndexImpl) t.getIndexes().get(0);

            assertEquals(IndexBuilder.PRIMARY_KEY_NAME, idx.getName());
            assertEquals(1, idx.getColumns().size());
            assertEquals("id", idx.getColumns().get(0).getName());
            assertTrue(idx.getColumns().get(0).isAscending());
            assertTrue(idx.isPrimaryKey());
            assertTrue(idx.isUnique());
            assertFalse(idx.shouldIgnoreNulls());
            assertNull(idx.getReference());

            t.addRow(2, "row2");
            t.addRow(1, "row1");
            t.addRow(3, "row3");

            Cursor c = t.newCursor()
                .withIndexByName(IndexBuilder.PRIMARY_KEY_NAME).toCursor();

            for (int i = 1; i <= 3; i++) {
                Map<String, Object> row = c.getNextRow();
                assertEquals(i, row.get("id"));
                assertEquals("row" + i, row.get("data"));
            }
            assertFalse(c.moveToNextRow());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource()
    void testIndexCreationSharedData(FileFormat fileFormat) throws Exception {
        try (Database db = TestUtil.create(fileFormat)) {
            Table t = DatabaseBuilder.newTable("TestTable")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("data", DataType.TEXT))
                .withPrimaryKey("id")
                .addIndex(DatabaseBuilder.newIndex("Index1").withColumns("id"))
                .addIndex(DatabaseBuilder.newIndex("Index2").withColumns("id"))
                .addIndex(DatabaseBuilder.newIndex("Index3").withColumns(false, "id"))
                .toTable(db);

            assertEquals(4, t.getIndexes().size());
            IndexImpl idx = (IndexImpl) t.getIndexes().get(0);

            assertEquals(IndexBuilder.PRIMARY_KEY_NAME, idx.getName());
            assertEquals(1, idx.getColumns().size());
            assertEquals("id", idx.getColumns().get(0).getName());
            assertTrue(idx.getColumns().get(0).isAscending());
            assertTrue(idx.isPrimaryKey());
            assertTrue(idx.isUnique());
            assertFalse(idx.shouldIgnoreNulls());
            assertNull(idx.getReference());

            IndexImpl idx1 = (IndexImpl) t.getIndexes().get(1);
            IndexImpl idx2 = (IndexImpl) t.getIndexes().get(2);
            IndexImpl idx3 = (IndexImpl) t.getIndexes().get(3);

            assertNotSame(idx.getIndexData(), idx1.getIndexData());
            assertSame(idx1.getIndexData(), idx2.getIndexData());
            assertNotSame(idx2.getIndexData(), idx3.getIndexData());

            t.addRow(2, "row2");
            t.addRow(1, "row1");
            t.addRow(3, "row3");

            Cursor c = t.newCursor()
                .withIndexByName(IndexBuilder.PRIMARY_KEY_NAME).toCursor();

            for (int i = 1; i <= 3; i++) {
                Map<String, Object> row = c.getNextRow();
                assertEquals(i, row.get("id"));
                assertEquals("row" + i, row.get("data"));
            }
            assertFalse(c.moveToNextRow());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.INDEX, readOnly = true)
    void testGetForeignKeyIndex(TestDb testDb) throws Exception {

        try (Database db = testDb.open()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            IndexImpl t2t1 = (IndexImpl) t1.getIndex("Table2Table1");
            IndexImpl t3t1 = (IndexImpl) t1.getIndex("Table3Table1");

            assertTrue(t2t1.isForeignKey());
            assertNotNull(t2t1.getReference());
            assertFalse(t2t1.getReference().isPrimaryTable());
            assertFalse(t2t1.getReference().isCascadeUpdates());
            assertTrue(t2t1.getReference().isCascadeDeletes());
            doCheckForeignKeyIndex(t2t1, t2);

            assertTrue(t3t1.isForeignKey());
            assertNotNull(t3t1.getReference());
            assertFalse(t3t1.getReference().isPrimaryTable());
            assertTrue(t3t1.getReference().isCascadeUpdates());
            assertFalse(t3t1.getReference().isCascadeDeletes());
            doCheckForeignKeyIndex(t3t1, t3);

            Index t1pk = t1.getIndex(IndexBuilder.PRIMARY_KEY_NAME);
            assertNotNull(t1pk);
            assertNull(((IndexImpl) t1pk).getReference());
            assertNull(t1pk.getReferencedIndex());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource()
    void testConstraintViolation(FileFormat fileFormat) throws Exception {

        try (Database db = TestUtil.create(fileFormat)) {
            Table t = DatabaseBuilder.newTable("TestTable")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG))
                .addColumn(DatabaseBuilder.newColumn("data", DataType.TEXT))
                .withPrimaryKey("id")
                .addIndex(DatabaseBuilder.newIndex("data_ind")
                    .withColumns("data").withUnique())
                .toTable(db);

            for (int i = 0; i < 5; i++) {
                t.addRow(i, "row" + i);
            }

            assertThrows(ConstraintViolationException.class, () -> t.addRow(3, "badrow"));

            assertEquals(5, t.getRowCount());

            List<Row> expectedRows = TestUtil.createExpectedTable(TestUtil.createExpectedRow("id", 0, "data", "row0"),
                    TestUtil.createExpectedRow("id", 1, "data", "row1"),
                    TestUtil.createExpectedRow("id", 2, "data", "row2"),
                    TestUtil.createExpectedRow("id", 3, "data", "row3"),
                    TestUtil.createExpectedRow("id", 4, "data", "row4"));

            TestUtil.assertTable(expectedRows, t);

            IndexCursor pkCursor = CursorBuilder.createPrimaryKeyCursor(t);
            TestUtil.assertCursor(expectedRows, pkCursor);

            TestUtil.assertCursor(expectedRows, CursorBuilder.createCursor(t.getIndex("data_ind")));

            List<Object[]> batch = new ArrayList<>();
            batch.add(new Object[] {5, "row5"});
            batch.add(new Object[] {6, "row6"});
            batch.add(new Object[] {7, "row2"});
            batch.add(new Object[] {8, "row8"});

            BatchUpdateException buex = assertThrows(BatchUpdateException.class, () -> t.addRows(batch));
            assertInstanceOf(ConstraintViolationException.class, buex.getCause());
            assertEquals(2, buex.getUpdateCount());

            expectedRows = new ArrayList<>(expectedRows);
            expectedRows.add(TestUtil.createExpectedRow("id", 5, "data", "row5"));
            expectedRows.add(TestUtil.createExpectedRow("id", 6, "data", "row6"));

            TestUtil.assertTable(expectedRows, t);

            TestUtil.assertCursor(expectedRows, pkCursor);

            TestUtil.assertCursor(expectedRows, CursorBuilder.createCursor(t.getIndex("data_ind")));

            pkCursor.findFirstRowByEntry(4);
            Row row4 = pkCursor.getCurrentRow();

            row4.put("id", 3);

            assertThrows(ConstraintViolationException.class, () -> t.updateRow(row4));

            TestUtil.assertTable(expectedRows, t);

            TestUtil.assertCursor(expectedRows, pkCursor);

            TestUtil.assertCursor(expectedRows, CursorBuilder.createCursor(t.getIndex("data_ind")));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource()
    void testAutoNumberRecover(FileFormat fileFormat) throws Exception {
        try (Database db = TestUtil.create(fileFormat)) {
            Table t = DatabaseBuilder.newTable("TestTable")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(DatabaseBuilder.newColumn("data", DataType.TEXT))
                .withPrimaryKey("id")
                .addIndex(DatabaseBuilder.newIndex("data_ind")
                    .withColumns("data").withUnique())
                .toTable(db);

            for (int i = 1; i < 3; i++) {
                t.addRow(null, "row" + i);
            }

            assertThrows(ConstraintViolationException.class, () -> t.addRow(null, "row1"));

            t.addRow(null, "row3");

            assertEquals(3, t.getRowCount());

            List<Row> expectedRows = TestUtil.createExpectedTable(
                    TestUtil.createExpectedRow("id", 1, "data", "row1"),
                    TestUtil.createExpectedRow("id", 2, "data", "row2"),
                    TestUtil.createExpectedRow("id", 3, "data", "row3"));

            TestUtil.assertTable(expectedRows, t);

            IndexCursor pkCursor = CursorBuilder.createPrimaryKeyCursor(t);
            TestUtil.assertCursor(expectedRows, pkCursor);

            TestUtil.assertCursor(expectedRows, CursorBuilder.createCursor(t.getIndex("data_ind")));

            List<Object[]> batch = new ArrayList<>();
            batch.add(new Object[] {null, "row4"});
            batch.add(new Object[] {null, "row5"});
            batch.add(new Object[] {null, "row3"});

            BatchUpdateException buex = assertThrows(BatchUpdateException.class, () -> t.addRows(batch));
            assertInstanceOf(ConstraintViolationException.class, buex.getCause());
            assertEquals(2, buex.getUpdateCount());

            expectedRows = new ArrayList<>(expectedRows);
            expectedRows.add(TestUtil.createExpectedRow("id", 4, "data", "row4"));
            expectedRows.add(TestUtil.createExpectedRow("id", 5, "data", "row5"));

            TestUtil.assertTable(expectedRows, t);

            TestUtil.assertCursor(expectedRows, pkCursor);

            TestUtil.assertCursor(expectedRows, CursorBuilder.createCursor(t.getIndex("data_ind")));
        }

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.BINARY_INDEX)
    void testBinaryIndex(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table table = db.getTable("Test");

            Index idx = table.getIndex("BinAscIdx");
            doTestBinaryIndex(idx, "BinAsc", false);

            idx = table.getIndex("BinDscIdx");
            doTestBinaryIndex(idx, "BinDsc", true);
        }
    }

    private static void doTestBinaryIndex(Index idx, String colName, boolean forward) throws Exception {
        IndexCursor ic = CursorBuilder.createCursor(idx);

        for (Row row : idx.getTable().getDefaultCursor().newIterable().withForward(forward)) {
            int id = row.getInt("ID");
            byte[] data = row.getBytes(colName);

            boolean found = false;
            for (Row idxRow : ic.newEntryIterable(data)) {

                assertArrayEquals(data, idxRow.getBytes(colName));
                if (id == idxRow.getInt("ID")) {
                    found = true;
                }
            }

            assertTrue(found);
        }
    }

    private void doCheckForeignKeyIndex(Index ia, Table tb) throws Exception {
        IndexImpl ib = (IndexImpl) ia.getReferencedIndex();
        assertNotNull(ib);
        assertSame(tb, ib.getTable());

        assertNotNull(ib.getReference());
        assertSame(ia, ib.getReferencedIndex());
        assertTrue(ib.getReference().isPrimaryTable());
    }

    private void checkIndexColumns(Table table, String... idxInfo) {
        Map<String, String> expectedIndexes = new HashMap<>();
        for (int i = 0; i < idxInfo.length; i += 2) {
            expectedIndexes.put(idxInfo[i], idxInfo[i + 1]);
        }

        for (Index idx : table.getIndexes()) {
            String colName = expectedIndexes.get(idx.getName());
            assertEquals(1, idx.getColumns().size());
            assertEquals(colName, idx.getColumns().get(0).getName());
            if ("PrimaryKey".equals(idx.getName())) {
                assertTrue(idx.isPrimaryKey());
            } else {
                assertFalse(idx.isPrimaryKey());
            }
        }
    }

}
