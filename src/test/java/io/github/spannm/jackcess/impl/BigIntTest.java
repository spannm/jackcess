package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.DatabaseBuilder.*;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class BigIntTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testBigInt(FileFormat fileFormat) throws IOException {
        JetFormat format = DatabaseImpl.getFileFormatDetails(fileFormat)
            .getFormat();

        if (!format.isSupportedDataType(DataType.BIG_INT)) {
            return;
        }

        try (Database db = createDbMem(fileFormat)) {
            Table t = newTable("Test")
                .addColumn(newColumn("id", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.TEXT))
                .addColumn(newColumn("num1", DataType.BIG_INT))
                .addIndex(newIndex("idx").withColumns("num1"))
                .toTable(db);

            long[] vals = new long[] {0L, -10L, 3844L, -45309590834L, 50392084913L, 65000L, -6489273L};

            List<Map<String, Object>> expectedTable = new ArrayList<>();

            int idx = 1;
            for (long lng : vals) {
                t.addRow(Column.AUTO_NUMBER, "" + lng, lng);

                expectedTable.add(TestUtil.createExpectedRow(
                    "id", idx++,
                    "data1", "" + lng,
                    "num1", lng));
            }

            expectedTable.sort(Comparator.comparingLong(r -> (Long) r.get("num1")));

            Cursor c = t.newCursor().withIndexByName("idx").toIndexCursor();

            TestUtil.assertCursor(expectedTable, c);
        }
    }
}
