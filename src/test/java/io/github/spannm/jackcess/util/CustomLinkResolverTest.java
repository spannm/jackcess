package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

class CustomLinkResolverTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCustomLinkResolver(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            db.setLinkResolver(new TestLinkResolver());

            db.createLinkedTable("Table1", "testFile1.txt", "Table1");
            db.createLinkedTable("Table2", "testFile2.txt", "OtherTable2");
            db.createLinkedTable("Table3", "missingFile3.txt", "MissingTable3");
            db.createLinkedTable("Table4", "testFile2.txt", "MissingTable4");

            Table t1 = db.getTable("Table1");
            assertNotNull(t1);
            assertNotSame(db, t1.getDatabase());

            TestUtil.assertTable(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("id", 0, "data1", "row0"),
                TestUtil.createExpectedRow("id", 1, "data1", "row1"),
                TestUtil.createExpectedRow("id", 2, "data1", "row2")),
                t1);

            Table t2 = db.getTable("Table2");
            assertNotNull(t2);
            assertNotSame(db, t2.getDatabase());

            TestUtil.assertTable(TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("id", 3, "data2", "row3"),
                TestUtil.createExpectedRow("id", 4, "data2", "row4"),
                TestUtil.createExpectedRow("id", 5, "data2", "row5")),
                t2);

            assertNull(db.getTable("Table4"));

            assertThrows(FileNotFoundException.class, () -> db.getTable("Table3"));
        }
    }

    private static class TestLinkResolver extends CustomLinkResolver {
        private TestLinkResolver() {
            super(DEFAULT_FORMAT, true, DEFAULT_TEMP_DIR);
        }

        @Override
        protected Object loadCustomFile(
            Database linkerDb, String linkeeFileName) {
            return "testFile1.txt".equals(linkeeFileName)
                || "testFile2.txt".equals(linkeeFileName) ? linkeeFileName : null;
        }

        @Override
        protected boolean loadCustomTable(
            Database tempDb, Object customFile, String tableName)
            throws IOException {
            if ("Table1".equals(tableName)) {

                assertEquals("testFile1.txt", customFile);
                Table t = new TableBuilder(tableName)
                    .addColumn(new ColumnBuilder("id", DataType.LONG))
                    .addColumn(new ColumnBuilder("data1", DataType.TEXT))
                    .toTable(tempDb);

                for (int i = 0; i < 3; i++) {
                    t.addRow(i, "row" + i);
                }

                return true;

            } else if ("OtherTable2".equals(tableName)) {

                assertEquals("testFile2.txt", customFile);
                Table t = new TableBuilder(tableName)
                    .addColumn(new ColumnBuilder("id", DataType.LONG))
                    .addColumn(new ColumnBuilder("data2", DataType.TEXT))
                    .toTable(tempDb);

                for (int i = 3; i < 6; i++) {
                    t.addRow(i, "row" + i);
                }

                return true;

            } else if ("Table4".equals(tableName)) {

                assertEquals("testFile2.txt", customFile);
                return false;
            }

            return false;
        }

        @Override
        protected Database createTempDb(Object customFile, FileFormat format,
            boolean inMemory, Path tempDir, boolean readOnly) throws IOException {
            inMemory = "testFile1.txt".equals(customFile);
            return super.createTempDb(customFile, format, inMemory, tempDir, readOnly);
        }
    }
}
