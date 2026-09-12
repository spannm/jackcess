/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
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
package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.DatabaseBuilder.newColumn;
import static io.github.spannm.jackcess.DatabaseBuilder.newTable;
import static io.github.spannm.jackcess.test.Basename.COMMON1;
import static io.github.spannm.jackcess.test.Basename.COMPLEX_DATA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.complex.ComplexValueForeignKey;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class AutoNumberTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void autoNumber(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = newTable("test")
                .addColumn(newColumn("a", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("b", DataType.TEXT))
                .toTable(db);

            doTestAutoNumber(table);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void autoNumberPK(TestDb testDB) throws Exception {
        try (Database db = testDB.openMem()) {
            Table table = db.getTable("Table3");

            doTestAutoNumber(table);
        }
    }

    private static void doTestAutoNumber(Table table) throws IOException {
        Object[] row = {null, "row1"};
        assertThat(table.addRow(row)).isSameAs(row);
        assertThat(((Integer) row[0]).intValue()).isEqualTo(1);
        row = table.addRow(13, "row2");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(2);
        row = table.addRow("flubber", "row3");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(3);

        table.reset();

        row = table.addRow(Column.AUTO_NUMBER, "row4");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(4);
        row = table.addRow(Column.AUTO_NUMBER, "row5");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(5);

        Object[] smallRow = {Column.AUTO_NUMBER};
        row = table.addRow(smallRow);
        assertThat(smallRow).isNotSameAs(row);
        assertThat(((Integer) row[0]).intValue()).isEqualTo(6);

        table.reset();

        List<? extends Map<String, Object>> expectedRows = TestUtil.createExpectedTable(
            TestUtil.createExpectedRow(
                "a", 1,
                "b", "row1"),
            TestUtil.createExpectedRow(
                "a", 2,
                "b", "row2"),
            TestUtil.createExpectedRow(
                "a", 3,
                "b", "row3"),
            TestUtil.createExpectedRow(
                "a", 4,
                "b", "row4"),
            TestUtil.createExpectedRow(
                "a", 5,
                "b", "row5"),
            TestUtil.createExpectedRow(
                "a", 6,
                "b", null));

        TestUtil.assertTable(expectedRows, table);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void autoNumberGuid(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = newTable("test")
                .addColumn(newColumn("a", DataType.GUID)
                    .withAutoNumber(true))
                .addColumn(newColumn("b", DataType.TEXT))
                .toTable(db);

            Object[] row = {null, "row1"};
            assertThat(table.addRow(row)).isSameAs(row);
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();
            row = table.addRow(13, "row2");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();
            row = table.addRow("flubber", "row3");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();

            Object[] smallRow = {Column.AUTO_NUMBER};
            row = table.addRow(smallRow);
            assertThat(smallRow).isNotSameAs(row);
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void insertLongAutoNumber(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = newTable("test")
                .addColumn(newColumn("a", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("b", DataType.TEXT))
                .toTable(db);

            doTestInsertLongAutoNumber(table);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void insertLongAutoNumberPK(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = newTable("test")
                .addColumn(newColumn("a", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("b", DataType.TEXT))
                .withPrimaryKey("a")
                .toTable(db);

            doTestInsertLongAutoNumber(table);
        }
    }

    private static void doTestInsertLongAutoNumber(Table table) throws IOException {
        assertThat(table.getDatabase().isAllowAutoNumberInsert()).isFalse();
        assertThat(table.isAllowAutoNumberInsert()).isFalse();

        Object[] row = {null, "row1"};
        assertThat(table.addRow(row)).isSameAs(row);
        assertThat(((Integer) row[0]).intValue()).isEqualTo(1);
        row = table.addRow(13, "row2");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(2);
        row = table.addRow("flubber", "row3");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(3);

        table.reset();

        table.setAllowAutoNumberInsert(true);
        assertThat(table.getDatabase().isAllowAutoNumberInsert()).isFalse();
        assertThat(table.isAllowAutoNumberInsert()).isTrue();

        Row row2 = CursorBuilder.findRow(
            table, Collections.singletonMap("a", 2));
        assertThat(row2.getString("b")).isEqualTo("row2");

        table.deleteRow(row2);

        row = table.addRow(Column.AUTO_NUMBER, "row4");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(4);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(4);

        row = table.addRow(2, "row2-redux");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(2);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(4);

        row2 = CursorBuilder.findRow(
            table, Collections.singletonMap("a", 2));
        assertThat(row2.getString("b")).isEqualTo("row2-redux");

        row = table.addRow(13, "row13-mindthegap");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(13);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(13);

        assertThrows(NumberFormatException.class, () -> table.addRow("not a number", "nope"));

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(13);

        table.addRow(-10, "non-positives are now allowed");

        row = table.addRow(Column.AUTO_NUMBER, "row14");
        assertThat(((Integer) row[0]).intValue()).isEqualTo(14);

        Row row13 = CursorBuilder.findRow(
            table, Collections.singletonMap("a", 13));
        assertThat(row13.getString("b")).isEqualTo("row13-mindthegap");

        row13.put("a", "45");
        row13 = table.updateRow(row13);
        assertThat(row13.get("a")).isEqualTo(45);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(45);

        row13.put("a", -1); // non-positives are now allowed
        table.updateRow(row13);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(45);

        row13.put("a", 55);

        // reset to db-level policy (which in this case is "false")
        table.setAllowAutoNumberInsert(null);

        row13 = table.updateRow(row13); // no change, as confirmed by...
        assertThat(row13.get("a")).isEqualTo(-1);

        assertThat(((TableImpl) table).getLastLongAutoNumber()).isEqualTo(45);

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void insertComplexAutoNumber(TestDb testDb) throws Exception {

        try (Database db = testDb.openMem()) {
            Table t1 = db.getTable("Table1");

            assertThat(t1.isAllowAutoNumberInsert()).isFalse();

            int lastAutoNum = ((TableImpl) t1).getLastComplexTypeAutoNumber();

            Object[] row = t1.addRow("arow");
            lastAutoNum++;
            checkAllComplexAutoNums(lastAutoNum, row);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(lastAutoNum);

            db.setAllowAutoNumberInsert(true);
            assertThat(db.isAllowAutoNumberInsert()).isTrue();
            assertThat(t1.isAllowAutoNumberInsert()).isTrue();

            row = t1.addRow("anotherrow");
            lastAutoNum++;
            checkAllComplexAutoNums(lastAutoNum, row);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(lastAutoNum);

            row = t1.addRow("row5", 5, null, null, 5, 5);
            checkAllComplexAutoNums(5, row);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(lastAutoNum);

            row = t1.addRow("row13", 13, null, null, 13, 13);
            checkAllComplexAutoNums(13, row);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(13);

            assertThrows(NumberFormatException.class, () ->
                t1.addRow("nope", "not a number"));

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(13);

            assertThrows(IOException.class, () ->
                t1.addRow("uh-uh", -10));

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(13);

            assertThrows(IOException.class, () ->
                t1.addRow("wut", 6, null, null, 40, 42));

            row = t1.addRow("morerows");
            checkAllComplexAutoNums(14, row);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(14);

            Row row13 = CursorBuilder.findRow(t1, Collections.singletonMap("id", "row13"));

            row13.put("VersionHistory_F5F8918F-0A3F-4DA9-AE71-184EE5012880", "45");
            row13.put("multi-value-data", "45");
            row13.put("attach-data", "45");

            final Row row13b = t1.updateRow(row13);
            checkAllComplexAutoNums(45, row13b);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(45);

            row13b.put("attach-data", -1);

            assertThrows(IOException.class, () -> t1.updateRow(row13b));

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(45);

            row13b.put("attach-data", 55);

            assertThrows(IOException.class, () -> t1.updateRow(row13b));

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(45);

            row13b.put("VersionHistory_F5F8918F-0A3F-4DA9-AE71-184EE5012880", 55);
            row13b.put("multi-value-data", 55);

            db.setAllowAutoNumberInsert(null);

            Row row13c = t1.updateRow(row13b);
            checkAllComplexAutoNums(45, row13c);

            assertThat(((TableImpl) t1).getLastComplexTypeAutoNumber()).isEqualTo(45);
        }
    }

    private static void checkAllComplexAutoNums(int expected, Object[] row) {
        assertThat(((ComplexValueForeignKey) row[1]).get()).isEqualTo(expected);
        assertThat(((ComplexValueForeignKey) row[4]).get()).isEqualTo(expected);
        assertThat(((ComplexValueForeignKey) row[5]).get()).isEqualTo(expected);
    }

    private static void checkAllComplexAutoNums(int expected, Row row) {
        assertThat(((Number) row.get("VersionHistory_F5F8918F-0A3F-4DA9-AE71-184EE5012880")).intValue()).isEqualTo(expected);
        assertThat(((Number) row.get("multi-value-data")).intValue()).isEqualTo(expected);
        assertThat(((Number) row.get("attach-data")).intValue()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void insertGuidAutoNumber(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table table = newTable("test").addColumn(newColumn("a", DataType.GUID).withAutoNumber(true)).addColumn(newColumn("b", DataType.TEXT)).toTable(db);

            db.setAllowAutoNumberInsert(true);
            table.setAllowAutoNumberInsert(false);
            assertThat(table.isAllowAutoNumberInsert()).isFalse();

            Object[] row = {null, "row1"};
            assertThat(table.addRow(row)).isSameAs(row);
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();
            row = table.addRow(13, "row2");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();
            row = table.addRow("flubber", "row3");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();

            Object[] smallRow = {Column.AUTO_NUMBER};
            row = table.addRow(smallRow);
            assertThat(smallRow).isNotSameAs(row);
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();

            table.setAllowAutoNumberInsert(null);
            assertThat(table.isAllowAutoNumberInsert()).isTrue();

            Row row2 = CursorBuilder.findRow(table, Collections.singletonMap("b", "row2"));
            assertThat(row2.getString("b")).isEqualTo("row2");

            String row2Guid = row2.getString("a");
            table.deleteRow(row2);

            row = table.addRow(Column.AUTO_NUMBER, "row4");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();

            row = table.addRow(row2Guid, "row2-redux");
            assertThat(row[0]).isEqualTo(row2Guid);

            row2 = CursorBuilder.findRow(table, Collections.singletonMap("a", row2Guid));
            assertThat(row2.getString("b")).isEqualTo("row2-redux");

            assertThrows(IOException.class, () -> table.addRow("not a guid", "nope"));

            row = table.addRow(Column.AUTO_NUMBER, "row5");
            assertThat(ColumnImpl.isGUIDValue(row[0])).isTrue();

            row2Guid = UUID.randomUUID().toString();
            row2.put("a", row2Guid);

            Row row2b = table.updateRow(row2);
            assertThat(row2b.get("a")).isEqualTo(row2Guid);

            row2b.put("a", "not a guid");

            assertThrows(IOException.class, () -> table.updateRow(row2b));

            table.setAllowAutoNumberInsert(false);

            Row row2c = table.updateRow(row2b);
            assertThat(ColumnImpl.isGUIDValue(row2c.get("a"))).isTrue();
            assertThat(row2c.get("a")).isNotEqualTo(row2Guid);
        }
    }

}
