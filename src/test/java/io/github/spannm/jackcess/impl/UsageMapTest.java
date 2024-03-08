package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.TestUtil.createString;
import static io.github.spannm.jackcess.test.TestUtil.openCopy;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dan Rollo Date: Mar 5, 2010 Time: 2:21:22 PM
 */
public class UsageMapTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getSupportedTestDbs")
    void testRead(TestDB testDB) throws Exception {
        int expectedFirstPage;
        int expectedLastPage;
        FileFormat expectedFileFormat = testDB.getExpectedFileFormat();
        if (FileFormat.V2000.equals(expectedFileFormat)) {
            expectedFirstPage = 743;
            expectedLastPage = 767;
        } else if (FileFormat.V2003.equals(expectedFileFormat)) {
            expectedFirstPage = 16;
            expectedLastPage = 799;
        } else if (FileFormat.V2007.equals(expectedFileFormat)) {
            expectedFirstPage = 94;
            expectedLastPage = 511;
        } else if (FileFormat.V2010.equals(expectedFileFormat)) {
            expectedFirstPage = 109;
            expectedLastPage = 511;
        } else {
            throw new IllegalAccessException("Unknown file format: " + expectedFileFormat);
        }
        try (Database db = DatabaseBuilder.open(testDB.getFile())) {
            UsageMap usageMap = UsageMap.read((DatabaseImpl) db,
                PageChannel.PAGE_GLOBAL_USAGE_MAP,
                PageChannel.ROW_GLOBAL_USAGE_MAP,
                true);
            assertEquals(expectedFirstPage, usageMap.getFirstPageNumber(), "Unexpected FirstPageNumber");
            assertEquals(expectedLastPage, usageMap.getLastPageNumber(), "Unexpected LastPageNumber");
        }
    }

    @Test
    void testGobalReferenceUsageMap() throws Exception {
        try (Database db = openCopy(
            FileFormat.V2000,
            new File(DIR_TEST_DATA, "V2000/testRefGlobalV2000.mdb"))) {
            Table t = new TableBuilder("Test2")
                .addColumn(new ColumnBuilder("id", DataType.LONG))
                .addColumn(new ColumnBuilder("data1", DataType.TEXT))
                .addColumn(new ColumnBuilder("data2", DataType.TEXT))
                .toTable(db);

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                List<Object[]> rows = new ArrayList<>();
                for (int i = 0; i < 300000; i++) {
                    String s1 = "r" + i + "-" + createString(100);
                    String s2 = "r" + i + "-" + createString(200);

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
}
