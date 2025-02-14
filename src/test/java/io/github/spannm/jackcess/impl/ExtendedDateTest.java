package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.DatabaseBuilder.*;
import static io.github.spannm.jackcess.test.Basename.EXT_DATE;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

class ExtendedDateTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(EXT_DATE)
    void testReadExtendedDate(TestDb testDb) throws Exception {
        ZoneId zoneId = ZoneId.of("America/New_York");
        DateTimeFormatter dtfNoTime = DateTimeFormatter.ofPattern("M/d/yyy", Locale.US);
        DateTimeFormatter dtfFull = DateTimeFormatter.ofPattern("M/d/yyy h:mm:ss.SSSSSSS a", Locale.US);

        try (Database db = testDb.openMem()) {
            db.setZoneId(zoneId);

            Table t = db.getTable("Table1");
            for (Row r : t) {
                LocalDateTime ldt = r.getLocalDateTime("DateExt");
                String str = r.getString("DateExtStr");

                if (ldt != null) {
                    String str1 = dtfNoTime.format(ldt);
                    String str2 = dtfFull.format(ldt);

                    assertTrue(str1.equals(str) || str2.equals(str));
                } else {
                    assertNull(str);
                }

            }

            Index idx = t.getIndex("DateExtAsc");
            IndexCodesTest.checkIndexEntries(testDb, t, idx);
            idx = t.getIndex("DateExtDesc");
            IndexCodesTest.checkIndexEntries(testDb, t, idx);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testWriteExtendedDate(FileFormat fileFormat) throws IOException {
        JetFormat format = DatabaseImpl.getFileFormatDetails(fileFormat).getFormat();

        if (!format.isSupportedDataType(DataType.EXT_DATE_TIME)) {
            return;
        }

        try (Database db = createDbMem(fileFormat)) {
            Table t = newTable("Test")
                .addColumn(newColumn("id", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.TEXT))
                .addColumn(newColumn("extDate", DataType.EXT_DATE_TIME))
                .addIndex(newIndex("idxAsc").withColumns("extDate"))
                .addIndex(newIndex("idxDesc").withColumns(false, "extDate"))
                .toTable(db);

            Object[] ldts = {LocalDate.of(2020, 6, 17), LocalDate.of(2021, 6, 14), LocalDateTime.of(2021, 6, 14, 12, 45), LocalDateTime.of(2021, 6, 14, 1, 45), LocalDateTime.of(2021, 6, 14, 22, 45,
                12, 345678900), LocalDateTime.of(1765, 6, 14, 12, 45), LocalDateTime.of(100, 6, 14, 12, 45, 00, 123456700), LocalDateTime.of(1265, 6, 14, 12, 45)
            };

            List<Map<String, Object>> expectedTable =
                new ArrayList<>();

            int idx = 1;
            for (Object ldt : ldts) {
                t.addRow(Column.AUTO_NUMBER, "" + ldt, ldt);

                LocalDateTime realLdt = (LocalDateTime) ColumnImpl.toInternalValue(
                    DataType.EXT_DATE_TIME, ldt, (DatabaseImpl) db);

                expectedTable.add(TestUtil.createExpectedRow(
                    "id", idx++,
                    "data1", "" + ldt,
                    "extDate", realLdt));
            }

            Comparator<Map<String, Object>> comp = (r1, r2) -> {
                LocalDateTime l1 = (LocalDateTime) r1.get("extDate");
                LocalDateTime l2 = (LocalDateTime) r2.get("extDate");
                return l1.compareTo(l2);
            };
            expectedTable.sort(comp);

            Cursor c = t.newCursor().withIndexByName("idxAsc").toIndexCursor();

            TestUtil.assertCursor(expectedTable, c);

            expectedTable.sort(comp.reversed());

            c = t.newCursor().withIndexByName("idxDesc").toIndexCursor();

            TestUtil.assertCursor(expectedTable, c);
        }
    }
}
