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

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

class RowFilterTest extends AbstractBaseTest {
    private static final String ID_COL = "id";
    private static final String COL1   = "col1";
    private static final String COL2   = "col2";
    private static final String COL3   = "col3";

    @Test
    void testFilter() {
        Row row0 = TestUtil.createExpectedRow(ID_COL, 0, COL1, "foo", COL2, 13, COL3, "bar");
        Row row1 = TestUtil.createExpectedRow(ID_COL, 1, COL1, "bar", COL2, 42, COL3, null);
        Row row2 = TestUtil.createExpectedRow(ID_COL, 2, COL1, "foo", COL2, 55, COL3, "bar");
        Row row3 = TestUtil.createExpectedRow(ID_COL, 3, COL1, "baz", COL2, 42, COL3, "bar");
        Row row4 = TestUtil.createExpectedRow(ID_COL, 4, COL1, "foo", COL2, 13, COL3, null);
        Row row5 = TestUtil.createExpectedRow(ID_COL, 5, COL1, "bla", COL2, 13, COL3, "bar");

        List<Row> rows = List.of(row0, row1, row2, row3, row4, row5);

        ColumnImpl testCol = new ColumnImpl(null, COL1, DataType.TEXT, 0, 0, 0) {
        };
        assertEquals(List.of(row0, row2, row4), toList(RowFilter.matchPattern(testCol, "foo").apply(rows)));
        assertEquals(List.of(row1, row3, row5), toList(RowFilter.invert(RowFilter.matchPattern(testCol, "foo")).apply(rows)));

        assertEquals(List.of(row0, row2, row4), toList(RowFilter.matchPattern(TestUtil.createExpectedRow(COL1, "foo")).apply(rows)));
        assertEquals(List.of(row0, row2), toList(RowFilter.matchPattern(TestUtil.createExpectedRow(COL1, "foo", COL3, "bar")).apply(rows)));
        assertEquals(List.of(row4), toList(RowFilter.matchPattern(TestUtil.createExpectedRow(COL1, "foo", COL3, null)).apply(rows)));
        assertEquals(List.of(row0, row4, row5), toList(RowFilter.matchPattern(TestUtil.createExpectedRow(COL2, 13)).apply(rows)));
        assertEquals(List.of(row1), toList(RowFilter.matchPattern(row1).apply(rows)));

        assertEquals(rows, toList(RowFilter.apply(null, rows)));
        assertEquals(List.of(row1), toList(RowFilter.apply(RowFilter.matchPattern(row1), rows)));
    }

}
