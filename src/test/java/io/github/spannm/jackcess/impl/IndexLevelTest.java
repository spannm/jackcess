/*
 * Copyright (c) 2026 James Ahlborn
 * Copyright (c) 2026 Markus Spann
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

import static io.github.spannm.jackcess.test.Basename.BIG_INDEX;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Tests that an index page records its level in the index tree, which is 0 at the leaves and rises toward the root.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IndexLevelTest extends AbstractBaseTest {

    private static final String EXTRA_TEXT = " some random text to fill out the index and make it fill up pages with"
        + " lots of extra bytes so that a few hundred rows are enough to give the index a root page with children under it";

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(BIG_INDEX)
    void writeDataPage_multiLevelIndex_writesLevel(TestDb testDb) throws Exception {
        try (Database db = testDb.openMem()) {
            TableImpl t = (TableImpl) db.getTable("Table1");
            IndexImpl index = t.getIndex("col1");

            Random rand = new Random(13L);
            for (int i = 0; i < 1000; i++) {
                t.addRow(rand.nextInt(Integer.MAX_VALUE) + EXTRA_TEXT, "this is some row data");
            }

            IndexData idxData = index.getIndexData();
            idxData.validate(false);

            db.flush();

            DatabaseImpl dbImpl = (DatabaseImpl) db;
            JetFormat format = dbImpl.getFormat();
            ByteBuffer buffer = dbImpl.getPageChannel().createPageBuffer();

            dbImpl.getPageChannel().readPage(buffer, idxData.getRootPageNumber());
            assertThat(buffer.get(0)).isEqualTo(PageTypes.INDEX_NODE);
            assertThat(getLevel(buffer, format)).as("root level").isGreaterThanOrEqualTo(1);

            // every other index page agrees: a leaf is 0 and a node is not
            int numNodes = 0;
            for (int pageNumber = 1; readPage(dbImpl, buffer, pageNumber); pageNumber++) {
                byte pageType = buffer.get(0);
                if (pageType == PageTypes.INDEX_LEAF) {
                    assertThat(getLevel(buffer, format)).as("page %d", pageNumber).isZero();
                } else if (pageType == PageTypes.INDEX_NODE) {
                    assertThat(getLevel(buffer, format)).as("page %d", pageNumber).isGreaterThanOrEqualTo(1);
                    numNodes++;
                }
            }

            assertThat(numNodes).isPositive();
        }
    }

    /**
     * Reads the given page into the buffer.
     *
     * @param db         database to read from
     * @param buffer     buffer to fill
     * @param pageNumber number of the page to read
     * @return {@code false} if the page lies beyond the end of the file
     */
    private static boolean readPage(DatabaseImpl db, ByteBuffer buffer, int pageNumber) {
        try {
            db.getPageChannel().readPage(buffer, pageNumber);
            return true;
        } catch (IOException | RuntimeException _ex) {
            return false;
        }
    }

    /**
     * Returns the level byte of the index page in the given buffer.
     *
     * @param buffer buffer holding an index page
     * @param format format of the database
     * @return the level of the page
     */
    private static int getLevel(ByteBuffer buffer, JetFormat format) {
        return ByteUtil.getUnsignedByte(buffer, format.OFFSET_INDEX_LEVEL);
    }
}
