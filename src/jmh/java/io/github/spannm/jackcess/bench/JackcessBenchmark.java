/*
 * Copyright (C) 2026- Markus Spann
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
package io.github.spannm.jackcess.bench;

import io.github.spannm.jackcess.Cursor;
import io.github.spannm.jackcess.CursorBuilder;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Index;
import io.github.spannm.jackcess.IndexCursor;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Ad-hoc JMH performance benchmarks for jackcess. Not part of the regular build/test cycle; run explicitly via
 * {@code mvn -Pjmh clean package && java -jar target/benchmarks.jar}.
 * <p>
 * Kept intentionally small (a handful of scenarios, one fork, few iterations) so the whole default suite finishes in
 * well under a minute or two, favouring "did we regress / where is time spent" trend signals over
 * publication-grade precision.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class JackcessBenchmark {

    private static final int    ROW_COUNT  = 5_000;
    private static final int    BATCH_SIZE = 200;
    private static final String TABLE_NAME = "t_bench";

    private static final String COL_ID      = "id";
    private static final String COL_NAME    = "name";
    private static final String COL_AMOUNT  = "amount";
    private static final String COL_CREATED = "created";
    private static final String COL_FLAG    = "flag";
    private static final String COL_NOTES   = "notes";

    private static final String NOTES_TEXT = "some notes for row that are long enough to exercise the memo/long-value code path";

    /**
     * Creates the {@link #TABLE_NAME} table with {@link #ROW_COUNT} rows: id (long, PK), name (text), amount
     * (double), created (date/time), flag (boolean), notes (memo).
     */
    private static Table createPopulatedTable(Database db) throws IOException {
        Table table = DatabaseBuilder.newTable(TABLE_NAME)
            .addColumn(DatabaseBuilder.newColumn(COL_ID, DataType.LONG))
            .addColumn(DatabaseBuilder.newColumn(COL_NAME, DataType.TEXT))
            .addColumn(DatabaseBuilder.newColumn(COL_AMOUNT, DataType.DOUBLE))
            .addColumn(DatabaseBuilder.newColumn(COL_CREATED, DataType.SHORT_DATE_TIME))
            .addColumn(DatabaseBuilder.newColumn(COL_FLAG, DataType.BOOLEAN))
            .addColumn(DatabaseBuilder.newColumn(COL_NOTES, DataType.MEMO))
            .addIndex(DatabaseBuilder.newPrimaryKey(COL_ID))
            .toTable(db);

        List<Object[]> rows = new ArrayList<>(ROW_COUNT);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < ROW_COUNT; i++) {
            rows.add(new Object[] {i, "name-" + i, i * 1.5d, now.plusMinutes(i), i % 2 == 0, NOTES_TEXT});
        }
        table.addRows(rows);
        return table;
    }

    /**
     * Builds a reusable batch of {@code count} row payloads with a placeholder id, so the insert benchmarks only
     * have to patch the id before inserting - keeping string building and boxing out of the measured code path.
     */
    private static List<Object[]> createInsertTemplate(int count) {
        List<Object[]> rows = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            rows.add(new Object[] {0, "name-template", 1.5d, now, Boolean.TRUE, NOTES_TEXT});
        }
        return rows;
    }

    /**
     * Read-only state: one database populated once per trial, reused (unmutated) across warmup + measurement
     * iterations for the scan/random-access/open benchmarks.
     */
    @State(Scope.Benchmark)
    public static class ReadState {
        Path          dbFile;
        Database      db;
        Table         table;
        Index         pkIndex;
        List<Integer> lookupIds;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            dbFile = Files.createTempFile("jackcess-bench-", ".accdb");
            Files.delete(dbFile);

            try (Database createDb = DatabaseBuilder.create(Database.FileFormat.V2010, dbFile.toFile())) {
                createPopulatedTable(createDb);
            }

            lookupIds = new ArrayList<>(ROW_COUNT);
            for (int i = 0; i < ROW_COUNT; i++) {
                lookupIds.add(i);
            }
            Collections.shuffle(lookupIds, new Random(42));

            db = DatabaseBuilder.open(dbFile.toFile());
            table = db.getTable(TABLE_NAME);
            pkIndex = table.getPrimaryKeyIndex();
        }

        @TearDown(Level.Trial)
        public void tearDown() throws IOException {
            db.close();
            Files.deleteIfExists(dbFile);
        }
    }

    /**
     * Mutable state: fresh database per trial, used exclusively by one of the write benchmarks so mutations do not
     * bleed into other benchmarks.
     */
    @State(Scope.Benchmark)
    public static class WriteState {
        Path           dbFile;
        Database       db;
        Table          table;
        int            nextId;
        List<Object[]> insertTemplate;
        Object[]       singleInsertTemplate;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            dbFile = Files.createTempFile("jackcess-bench-write-", ".accdb");
            Files.delete(dbFile);
            db = DatabaseBuilder.create(Database.FileFormat.V2010, dbFile.toFile());
            table = createPopulatedTable(db);
            nextId = ROW_COUNT;
            insertTemplate = createInsertTemplate(BATCH_SIZE);
            singleInsertTemplate = createInsertTemplate(1).get(0);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws IOException {
            db.close();
            Files.deleteIfExists(dbFile);
        }
    }

    @Benchmark
    public static void openDatabase(ReadState state, Blackhole bh) throws IOException {
        try (Database db = DatabaseBuilder.open(state.dbFile.toFile())) {
            bh.consume(db.getTable(TABLE_NAME));
        }
    }

    @Benchmark
    @OperationsPerInvocation(ROW_COUNT)
    public static void scanTable(ReadState state, Blackhole bh) throws IOException {
        Cursor cursor = CursorBuilder.createCursor(state.table);
        for (Row row : cursor) {
            bh.consume(row.getString(COL_NAME));
        }
    }

    @Benchmark
    @OperationsPerInvocation(ROW_COUNT)
    public static void randomAccessByPrimaryKey(ReadState state, Blackhole bh) throws IOException {
        IndexCursor cursor = CursorBuilder.createCursor(state.pkIndex);
        for (Integer id : state.lookupIds) {
            bh.consume(cursor.findRowByEntry(id));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public static void insertRows(WriteState state) throws IOException {
        for (Object[] row : state.insertTemplate) {
            row[0] = state.nextId++;
        }
        state.table.addRows(state.insertTemplate);
    }

    /**
     * Single-row counterpart to {@link #insertRows}, using {@link Table#addRow} instead of the batch
     * {@link Table#addRows} - for a fair per-operation comparison against {@link #updateRows} and
     * {@link #deleteRows}, which have no batch API to call into.
     */
    @Benchmark
    public static void insertRowSingle(WriteState state) throws IOException {
        state.singleInsertTemplate[0] = state.nextId++;
        state.table.addRow(state.singleInsertTemplate);
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public static void updateRows(WriteState state) throws IOException {
        Cursor cursor = CursorBuilder.createCursor(state.table);
        for (int i = 0; i < BATCH_SIZE && cursor.moveToNextRow(); i++) {
            Row row = cursor.getCurrentRow();
            row.put(COL_AMOUNT, (Double) row.get(COL_AMOUNT) + 1.0d);
            state.table.updateRow(row);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public static void deleteRows(WriteState state) throws IOException {
        Cursor cursor = CursorBuilder.createCursor(state.table);
        for (int i = 0; i < BATCH_SIZE && cursor.moveToNextRow(); i++) {
            cursor.deleteCurrentRow();
        }
    }
}
