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

import static io.github.spannm.jackcess.test.Basename.COMMON1;
import static io.github.spannm.jackcess.test.Basename.REF_GLOBAL;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UsageMapTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void testRead(TestDb testDB) throws Exception {
        int expectedFirstPage;
        int expectedLastPage;
        FileFormat expectedFileFormat = testDB.getExpectedFileFormat();
        if (FileFormat.V2000 == expectedFileFormat) {
            expectedFirstPage = 743;
            expectedLastPage = 767;
        } else if (FileFormat.V2003 == expectedFileFormat) {
            expectedFirstPage = 16;
            expectedLastPage = 799;
        } else if (FileFormat.V2007 == expectedFileFormat) {
            expectedFirstPage = 94;
            expectedLastPage = 511;
        } else if (FileFormat.V2010 == expectedFileFormat) {
            expectedFirstPage = 109;
            expectedLastPage = 511;
        } else {
            throw new IllegalAccessException("Unknown file format: " + expectedFileFormat);
        }
        try (Database db = testDB.openCopy()) {
            UsageMap usageMap = UsageMap.read((DatabaseImpl) db,
                PageChannel.PAGE_GLOBAL_USAGE_MAP,
                PageChannel.ROW_GLOBAL_USAGE_MAP,
                true);
            assertEquals(expectedFirstPage, usageMap.getFirstPageNumber(), "Unexpected FirstPageNumber");
            assertEquals(expectedLastPage, usageMap.getLastPageNumber(), "Unexpected LastPageNumber");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(REF_GLOBAL)
    void testGobalReferenceUsageMap(TestDb testDb) throws IOException {
        try (Database db = TestUtil.openCopy(FileFormat.V2000, testDb.getFile())) {
            Table t = new TableBuilder("Test2")
                .addColumn(new ColumnBuilder("id", DataType.LONG))
                .addColumn(new ColumnBuilder("data1", DataType.TEXT))
                .addColumn(new ColumnBuilder("data2", DataType.TEXT))
                .toTable(db);

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                List<Object[]> rows = new ArrayList<>();
                for (int i = 0; i < 300000; i++) {
                    String s1 = "r" + i + "-" + TestUtil.createString(100);
                    String s2 = "r" + i + "-" + TestUtil.createString(200);

                    rows.add(new Object[] {i, s1, s2});

                    if (i % 2000 == 0) {
                        t.addRows(rows);
                        rows.clear();
                    }
                }
            } finally {
                ((DatabaseImpl) db).getPageChannel().finishWrite();
            }
        }
    }

    @Test
    void testPromoteGlobalUsageMapToReference() throws Exception {
        Database db = createDb(FileFormat.V2003, false, false);
        File dbFile = db.getFile();

        Table t = new TableBuilder("Test")
            .addColumn(new ColumnBuilder("id", DataType.LONG))
            .addColumn(new ColumnBuilder("data1", DataType.TEXT))
            .addColumn(new ColumnBuilder("data2", DataType.TEXT))
            .toTable(db);

        // add enough rows to grow the database well beyond the inline global usage map's page-range limit, which
        // should force the global usage map to be promoted to a reference usage map
        int numRows = 20000;
        ((DatabaseImpl) db).getPageChannel().startWrite();
        try {
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < numRows; i++) {
                rows.add(new Object[] {i, "r" + i + "-" + TestUtil.createString(100), "r" + i + "-" + TestUtil.createString(200)});
                if (i % 2000 == 0) {
                    t.addRows(rows);
                    rows.clear();
                }
            }
            t.addRows(rows);
        } finally {
            ((DatabaseImpl) db).getPageChannel().finishWrite();
        }
        db.close();

        // reopen and verify the global usage map is now a reference map which covers the entire database (starting
        // from page 0), and that all the data is still readable
        try (Database db2 = DatabaseBuilder.open(dbFile)) {
            UsageMap gmap = UsageMap.read((DatabaseImpl) db2, PageChannel.PAGE_GLOBAL_USAGE_MAP, PageChannel.ROW_GLOBAL_USAGE_MAP, true);
            assertEquals("GlobalReferenceHandler", getHandlerName(gmap), "global usage map should be promoted to a reference map");
            assertEquals(0, gmap.getStartPage(), "global reference map should start at page 0");

            int count = 0;
            for (@SuppressWarnings("unused")
            Row r : db2.getTable("Test")) {
                count++;
            }
            assertEquals(numRows, count);
        }
    }

    private static String getHandlerName(UsageMap usageMap) throws Exception {
        Field f = UsageMap.class.getDeclaredField("handler");
        f.setAccessible(true);
        return f.get(usageMap).getClass().getSimpleName();
    }
}
