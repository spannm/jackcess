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

import static io.github.spannm.jackcess.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class DatabaseReadWriteTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void writeAndRead(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestWriteAndRead(db);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void writeAndReadInMem(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            doTestWriteAndRead(db);
        }
    }

    private static void doTestWriteAndRead(Database db) throws IOException {
        createTestTable(db);
        Object[] row = createTestRow();
        row[3] = null;
        Table table = db.getTable("Test");
        int count = 1000;
        ((DatabaseImpl) db).getPageChannel().startWrite();
        try {
            for (int i = 0; i < count; i++) {
                table.addRow(row);
            }
        } finally {
            ((DatabaseImpl) db).getPageChannel().finishWrite();
        }
        for (int i = 0; i < count; i++) {
            Map<String, Object> readRow = table.getNextRow();
            assertThat(readRow.get("A")).isEqualTo(row[0]);
            assertThat(readRow.get("B")).isEqualTo(row[1]);
            assertThat(readRow.get("C")).isEqualTo(row[2]);
            assertThat(readRow.get("D")).isEqualTo(row[3]);
            assertThat(readRow.get("E")).isEqualTo(row[4]);
            assertThat(readRow.get("F")).isEqualTo(row[5]);
            assertThat(readRow.get("G")).isEqualTo(row[6]);
            assertThat(readRow.get("H")).isEqualTo(row[7]);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void writeAndReadInBatch(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            createTestTable(db);
            int count = 1000;
            List<Object[]> rows = new ArrayList<>(count);
            Object[] row = createTestRow();
            for (int i = 0; i < count; i++) {
                rows.add(row);
            }
            Table table = db.getTable("Test");
            table.addRows(rows);
            for (int i = 0; i < count; i++) {
                Map<String, Object> readRow = table.getNextRow();
                assertThat(readRow.get("A")).isEqualTo(row[0]);
                assertThat(readRow.get("B")).isEqualTo(row[1]);
                assertThat(readRow.get("C")).isEqualTo(row[2]);
                assertThat(readRow.get("D")).isEqualTo(row[3]);
                assertThat(readRow.get("E")).isEqualTo(row[4]);
                assertThat(readRow.get("F")).isEqualTo(row[5]);
                assertThat(readRow.get("G")).isEqualTo(row[6]);
                assertThat(readRow.get("H")).isEqualTo(row[7]);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void updateRow(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            Table t = new TableBuilder("test")
                .addColumn(new ColumnBuilder("name", DataType.TEXT))
                .addColumn(new ColumnBuilder("id", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(new ColumnBuilder("data", DataType.TEXT)
                    .withLength(JetFormat.TEXT_FIELD_MAX_LENGTH))
                .toTable(db);

            for (int i = 0; i < 10; i++) {
                t.addRow("row" + i, Column.AUTO_NUMBER, "initial data");
            }

            Cursor c = CursorBuilder.createCursor(t);
            c.reset();
            c.moveNextRows(2);
            Map<String, Object> row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row1",
                    "id", 2,
                    "data", "initial data"));

            Map<String, Object> newRow = createExpectedRow(
                "name", Column.KEEP_VALUE,
                "id", Column.AUTO_NUMBER,
                "data", "new data");
            assertThat(c.updateCurrentRowFromMap(newRow)).isSameAs(newRow);
            assertThat(newRow).isEqualTo(createExpectedRow("name", "row1",
                    "id", 2,
                    "data", "new data"));

            c.moveNextRows(3);
            row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row4",
                    "id", 5,
                    "data", "initial data"));

            c.updateCurrentRow(Column.KEEP_VALUE, Column.AUTO_NUMBER, "a larger amount of new data");

            c.reset();
            c.moveNextRows(2);
            row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row1",
                    "id", 2,
                    "data", "new data"));

            c.moveNextRows(3);
            row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row4",
                    "id", 5,
                    "data", "a larger amount of new data"));

            t.reset();

            String str = createString(100);
            for (int i = 10; i < 50; i++) {
                t.addRow("row" + i, Column.AUTO_NUMBER, "big data_" + str);
            }

            c.reset();
            c.moveNextRows(9);
            row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row8",
                    "id", 9,
                    "data", "initial data"));

            String newText = "updated big data_" + createString(200);

            c.setCurrentRowValue(t.getColumn("data"), newText);

            c.reset();
            c.moveNextRows(9);
            row = c.getCurrentRow();

            assertThat(row).isEqualTo(createExpectedRow("name", "row8",
                    "id", 9,
                    "data", newText));

            List<Row> rows = toList(t);
            assertThat(rows.size()).isEqualTo(50);

            for (Row r : rows) {
                r.put("data", "final data " + r.get("id"));
            }

            for (Row r : rows) {
                assertThat(t.updateRow(r)).isSameAs(r);
            }

            t.reset();

            for (Row r : t) {
                assertThat(r.get("data")).isEqualTo("final data " + r.get("id"));
            }
        }

    }

    @Test
    void dateMath() {
        long now = System.currentTimeMillis();

        // test around current time
        doTestDateMath(now);

        // test around the unix epoch
        doTestDateMath(0L);

        // test around the access epoch
        doTestDateMath(-ColumnImpl.MILLIS_BETWEEN_EPOCH_AND_1900);
    }

    private static void doTestDateMath(long testTime) {
        final long timeRange = 100000000L;
        final long timeStep = 37L;

        for (long time = testTime - timeRange; time < testTime + timeRange; time += timeStep) {
            double accTime = ColumnImpl.toLocalDateDouble(time);
            long newTime = ColumnImpl.fromLocalDateDouble(accTime);
            assertThat(newTime).isEqualTo(time);

            Instant inst = Instant.ofEpochMilli(time);
            LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneOffset.UTC);

            accTime = ColumnImpl.toDateDouble(ldt);
            LocalDateTime newLdt = ColumnImpl.ldtFromLocalDateDouble(accTime);
            assertThat(newLdt).isEqualTo(ldt);
        }
    }
}
