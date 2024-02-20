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

package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.TestUtil.countRows;

import io.github.spannm.jackcess.impl.IndexImpl;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author james
 */
class BigIndexTest extends AbstractBaseTest {

    @Test
    void testComplexIndex() throws Exception {
        for (TestDB testDB : TestDB.getSupportedTestDbsForRead(Basename.COMP_INDEX)) {
            try (// this file has an index with "compressed" entries and node pages
            Database db = testDB.openMem()) {
                TableImpl t = (TableImpl) db.getTable("Table1");
                IndexImpl index = t.getIndex("CD_AGENTE");
                assertFalse(index.isInitialized());
                assertEquals(512, countRows(t));
                assertEquals(512, index.getIndexData().getEntryCount());
            }
        }
    }

    @Test
    void testBigIndex() throws Exception {
        for (TestDB testDB : TestDB.getSupportedTestDbs(Basename.BIG_INDEX)) {
            // this file has an index with "compressed" entries and node pages
            Database db = testDB.openMem();
            TableImpl t = (TableImpl) db.getTable("Table1");
            IndexImpl index = t.getIndex("col1");
            assertFalse(index.isInitialized());
            assertEquals(0, countRows(t));
            assertEquals(0, index.getIndexData().getEntryCount());
            db.close();

            setTestAutoSync(false);
            try {

                String extraText
                    = " some random text to fill out the index and make it fill up pages with lots of extra bytes "
                    + "so i will keep typing until i think that i probably have enough text in the index entry so that i do not need to add as many entries in order";

                // copy to temp file and attempt to edit
                db = testDB.openMem();
                t = (TableImpl) db.getTable("Table1");
                index = t.getIndex("col1");

                // add 2,000 (pseudo) random entries to the table
                Random rand = new Random(13L);
                for (int i = 0; i < 2000; ++i) {
                    if (i == 850 || i == 1850) {
                        int end = i + 50;
                        List<Object[]> rows = new ArrayList<>(50);
                        for (; i < end; ++i) {
                            int nextInt = rand.nextInt(Integer.MAX_VALUE);
                            String nextVal = nextInt + extraText;
                            if ((i + 1) % 333 == 0) {
                                nextVal = null;
                            }
                            rows.add(new Object[] {nextVal, "this is some row data " + nextInt});
                        }
                        t.addRows(rows);
                        i--;
                    } else {
                        int nextInt = rand.nextInt(Integer.MAX_VALUE);
                        String nextVal = nextInt + extraText;
                        if ((i + 1) % 333 == 0) {
                            nextVal = null;
                        }
                        t.addRow(nextVal, "this is some row data " + nextInt);
                    }
                }

                index.getIndexData().validate(false);

                db.flush();
                t = null;
                System.gc();

                t = (TableImpl) db.getTable("Table1");
                index = t.getIndex("col1");

                // make sure all entries are there and correctly ordered
                String firstValue = "      ";
                String prevValue = firstValue;
                int rowCount = 0;
                List<String> firstTwo = new ArrayList<>();
                for (Row row : CursorBuilder.createCursor(index)) {
                    String origVal = row.getString("col1");
                    String val = origVal;
                    if (val == null) {
                        val = firstValue;
                    }
                    assertTrue(prevValue.compareTo(val) <= 0, prevValue + " <= " + val + " " + rowCount);
                    if (firstTwo.size() < 2) {
                        firstTwo.add(origVal);
                    }
                    prevValue = val;
                    rowCount++;
                }

                assertEquals(2000, rowCount);

                index.getIndexData().validate(false);

                // delete an entry in the middle
                Cursor cursor = CursorBuilder.createCursor(index);
                for (int i = 0; i < rowCount / 2; ++i) {
                    assertTrue(cursor.moveToNextRow());
                }
                cursor.deleteCurrentRow();
                rowCount--;

                // remove all but the first two entries (from the end)
                cursor.afterLast();
                for (int i = 0; i < rowCount - 2; ++i) {
                    assertTrue(cursor.moveToPreviousRow());
                    cursor.deleteCurrentRow();
                }

                index.getIndexData().validate(false);

                List<String> found = new ArrayList<>();
                for (Row row : CursorBuilder.createCursor(index)) {
                    found.add(row.getString("col1"));
                }

                assertEquals(firstTwo, found);

                // remove remaining entries
                cursor = CursorBuilder.createCursor(t);
                for (int i = 0; i < 2; ++i) {
                    assertTrue(cursor.moveToNextRow());
                    cursor.deleteCurrentRow();
                }

                assertFalse(cursor.moveToNextRow());
                assertFalse(cursor.moveToPreviousRow());

                index.getIndexData().validate(false);

                // add 50 (pseudo) random entries to the table
                rand = new Random(42L);
                for (int i = 0; i < 50; ++i) {
                    int nextInt = rand.nextInt(Integer.MAX_VALUE);
                    String nextVal = "some prefix " + nextInt + extraText;
                    if ((i + 1) % 3333 == 0) {
                        nextVal = null;
                    }
                    t.addRow(nextVal, "this is some row data " + nextInt);
                }

                index.getIndexData().validate(false);

                cursor = CursorBuilder.createCursor(index);
                while (cursor.moveToNextRow()) {
                    cursor.deleteCurrentRow();
                }

                index.getIndexData().validate(false);

                db.close();

            } finally {
                clearTestAutoSync();
            }
        }
    }

}
