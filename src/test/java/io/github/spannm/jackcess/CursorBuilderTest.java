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

import static io.github.spannm.jackcess.test.Basename.INDEX_CURSOR;

import io.github.spannm.jackcess.impl.IndexImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * @author James Ahlborn
 */
class CursorBuilderTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(INDEX_CURSOR)
    void test(TestDb testDb) throws Exception {
        try (Database db = CursorTest.createTestIndexTable(testDb)) {
            Table table = db.getTable("test");
            IndexImpl idx = (IndexImpl) table.getIndexes().get(0);

            Cursor expected = CursorBuilder.createCursor(table);

            Cursor found = table.newCursor().toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(idx);
            found = idx.newCursor().toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(idx);
            found = table.newCursor()
                .withIndexByName("id")
                .toCursor();
            assertCursor(expected, found);

            assertThrows(IllegalArgumentException.class, () -> table.newCursor().withIndexByName("foo"));

            expected = CursorBuilder.createCursor(idx);
            found = table.newCursor()
                .withIndexByColumns(table.getColumn("id"))
                .toCursor();
            assertCursor(expected, found);

            assertThrows(IllegalArgumentException.class, () -> table.newCursor()
                .withIndexByColumns(table.getColumn("value")));

            assertThrows(IllegalArgumentException.class, () -> table.newCursor()
                .withIndexByColumns(table.getColumn("id"), table.getColumn("value")));

            expected = CursorBuilder.createCursor(table);
            expected.beforeFirst();
            found = table.newCursor()
                .beforeFirst()
                .toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(table);
            expected.afterLast();
            found = table.newCursor()
                .afterLast()
                .toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(table);
            expected.moveNextRows(2);
            Cursor.Savepoint sp = expected.getSavepoint();
            found = table.newCursor()
                .afterLast()
                .restoreSavepoint(sp)
                .toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(idx);
            expected.moveNextRows(2);
            sp = expected.getSavepoint();
            found = idx.newCursor()
                .beforeFirst()
                .restoreSavepoint(sp)
                .toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(idx,
                idx.constructIndexRowFromEntry(3),
                null);
            found = idx.newCursor()
                .withStartEntry(3)
                .toCursor();
            assertCursor(expected, found);

            expected = CursorBuilder.createCursor(idx,
                idx.constructIndexRowFromEntry(3),
                false,
                idx.constructIndexRowFromEntry(7),
                false);
            found = idx.newCursor()
                .withStartEntry(3)
                .withStartRowInclusive(false)
                .withEndEntry(7)
                .withEndRowInclusive(false)
                .toCursor();
            assertCursor(expected, found);
        }
    }

    private static void assertCursor(Cursor expected, Cursor found) {
        assertSame(expected.getTable(), found.getTable());
        if (expected instanceof IndexCursor) {
            assertSame(((IndexCursor) expected).getIndex(),
                ((IndexCursor) found).getIndex());
        }

        assertEquals(expected.getSavepoint().getCurrentPosition(),
            found.getSavepoint().getCurrentPosition());
    }

}
