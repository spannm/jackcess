/*
Copyright (c) 2011 James Ahlborn

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

package io.github.spannm.jackcess.util;

import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Index;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.impl.RowImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author James Ahlborn
 */
class JoinerTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.INDEX)
    void testJoiner(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            Index t1t2 = t1.getIndex("Table2Table1");
            Index t1t3 = t1.getIndex("Table3Table1");

            Index t2t1 = t1t2.getReferencedIndex();
            assertSame(t2, t2t1.getTable());
            Joiner t2t1Join = Joiner.create(t2t1);

            assertSame(t2, t2t1Join.getFromTable());
            assertSame(t2t1, t2t1Join.getFromIndex());
            assertSame(t1, t2t1Join.getToTable());
            assertSame(t1t2, t2t1Join.getToIndex());

            doTestJoiner(t2t1Join, createT2T1Data());

            Index t3t1 = t1t3.getReferencedIndex();
            assertSame(t3, t3t1.getTable());
            Joiner t3t1Join = Joiner.create(t3t1);

            assertSame(t3, t3t1Join.getFromTable());
            assertSame(t3t1, t3t1Join.getFromIndex());
            assertSame(t1, t3t1Join.getToTable());
            assertSame(t1t3, t3t1Join.getToIndex());

            doTestJoiner(t3t1Join, createT3T1Data());

            doTestJoinerDelete(t2t1Join);
        }
    }

    private static void doTestJoiner(
        Joiner join, Map<Integer, List<Row>> expectedData) throws Exception {
        final Set<String> colNames = Set.of("id", "data");

        Joiner revJoin = join.createReverse();
        for (Row row : join.getFromTable()) {
            Integer id = row.getInt("id");

            List<Row> joinedRows = join.findRows(row).stream()
                .collect(Collectors.toList());

            List<Row> expectedRows = expectedData.get(id);
            assertEquals(expectedData.get(id), joinedRows);

            if (!expectedRows.isEmpty()) {
                assertTrue(join.hasRows(row));
                assertEquals(expectedRows.get(0), join.findFirstRow(row));

                assertEquals(row, revJoin.findFirstRow(expectedRows.get(0)));
            } else {
                assertFalse(join.hasRows(row));
                assertNull(join.findFirstRow(row));
            }

            List<Row> expectedRows2 = new ArrayList<>();
            for (Row tmpRow : expectedRows) {
                Row tmpRow2 = new RowImpl(tmpRow);
                tmpRow2.keySet().retainAll(colNames);
                expectedRows2.add(tmpRow2);
            }

            joinedRows = join.findRows(row).withColumnNames(colNames)
                .stream().collect(Collectors.toList());

            assertEquals(expectedRows2, joinedRows);

            if (!expectedRows2.isEmpty()) {
                assertEquals(expectedRows2.get(0), join.findFirstRow(row, colNames));
            } else {
                assertNull(join.findFirstRow(row, colNames));
            }
        }
    }

    private static void doTestJoinerDelete(Joiner t2t1Join) throws Exception {
        assertEquals(4, countRows(t2t1Join.getToTable()));

        Row row = createExpectedRow("id", 1);
        assertTrue(t2t1Join.hasRows(row));

        assertTrue(t2t1Join.deleteRows(row));

        assertFalse(t2t1Join.hasRows(row));
        assertFalse(t2t1Join.deleteRows(row));

        assertEquals(2, countRows(t2t1Join.getToTable()));
        for (Row t1Row : t2t1Join.getToTable()) {
            assertNotEquals(1, t1Row.get("otherfk1"));
        }
    }

    private static Map<Integer, List<Row>> createT2T1Data() {
        Map<Integer, List<Row>> data = new HashMap<>();

        data.put(0,
            createExpectedTable(
                createExpectedRow("id", 0, "otherfk1", 0, "otherfk2", 10,
                    "data", "baz0", "otherfk3", 0)));

        data.put(1,
            createExpectedTable(
                createExpectedRow("id", 1, "otherfk1", 1, "otherfk2", 11,
                    "data", "baz11", "otherfk3", 0),
                createExpectedRow("id", 2, "otherfk1", 1, "otherfk2", 11,
                    "data", "baz11-2", "otherfk3", 0)));

        data.put(2,
            createExpectedTable(
                createExpectedRow("id", 3, "otherfk1", 2, "otherfk2", 13,
                    "data", "baz13", "otherfk3", 0)));

        return data;
    }

    private static Map<Integer, List<Row>> createT3T1Data() {
        Map<Integer, List<Row>> data = new HashMap<>();

        data.put(10,
            createExpectedTable(
                createExpectedRow("id", 0, "otherfk1", 0, "otherfk2", 10,
                    "data", "baz0", "otherfk3", 0)));

        data.put(11,
            createExpectedTable(
                createExpectedRow("id", 1, "otherfk1", 1, "otherfk2", 11,
                    "data", "baz11", "otherfk3", 0),
                createExpectedRow("id", 2, "otherfk1", 1, "otherfk2", 11,
                    "data", "baz11-2", "otherfk3", 0)));

        data.put(12,
            createExpectedTable());

        data.put(13,
            createExpectedTable(
                createExpectedRow("id", 3, "otherfk1", 2, "otherfk2", 13,
                    "data", "baz13", "otherfk3", 0)));

        return data;
    }

}
