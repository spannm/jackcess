/*
Copyright (c) 2018 James Ahlborn

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

import static io.github.spannm.jackcess.DatabaseBuilder.newColumn;
import static io.github.spannm.jackcess.DatabaseBuilder.newTable;
import static io.github.spannm.jackcess.test.TestUtil.assertSameDate;
import static io.github.spannm.jackcess.test.TestUtil.createMem;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestDbs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 * @author James Ahlborn
 */
class LocalDateTimeTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDbs#getFileformats()")
    void testWriteAndReadLocalDate(FileFormat fileFormat) throws Exception {
        try (Database db = createMem(fileFormat)) {
            db.setDateTimeType(DateTimeType.LOCAL_DATE_TIME);

            Table table = newTable("test")
                .addColumn(newColumn("name", DataType.TEXT))
                .addColumn(newColumn("date", DataType.SHORT_DATE_TIME))
                .toTable(db);

            // since jackcess does not really store millis, shave them off before
            // storing the current date/time
            long curTimeNoMillis = System.currentTimeMillis() / 1000L;
            curTimeNoMillis *= 1000L;

            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            List<Date> dates = new ArrayList<>(Arrays.asList(
                df.parse("19801231 00:00:00"),
                df.parse("19930513 14:43:27"),
                null,
                df.parse("20210102 02:37:00"),
                new Date(curTimeNoMillis)));

            Calendar c = Calendar.getInstance();
            for (int year = 1801; year < 2050; year += 3) {
                for (int month = 0; month <= 12; ++month) {
                    for (int day = 1; day < 29; day += 3) {
                        c.clear();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        dates.add(c.getTime());
                    }
                }
            }

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                for (Date d : dates) {
                    table.addRow("row " + d, d);
                }
            } finally {
                ((DatabaseImpl) db).getPageChannel().finishWrite();
            }

            List<LocalDateTime> foundDates = new ArrayList<>();
            for (Row row : table) {
                foundDates.add(row.getLocalDateTime("date"));
            }

            assertEquals(dates.size(), foundDates.size());
            for (int i = 0; i < dates.size(); i++) {
                Date expected = dates.get(i);
                LocalDateTime found = foundDates.get(i);
                assertSameDate(expected, found);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDbs#getFileformats()")
    void testAncientLocalDates1(FileFormat fileFormat) throws Exception {
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        List<String> dates = List.of("1582-10-15", "1582-10-14", "1492-01-10", "1392-01-10");

        try (Database db = createMem(fileFormat)) {
            db.setZoneId(ZoneId.of("America/New_York"));
            db.setDateTimeType(DateTimeType.LOCAL_DATE_TIME);

            Table table = newTable("test")
                .addColumn(newColumn("name", DataType.TEXT))
                .addColumn(newColumn("date", DataType.SHORT_DATE_TIME))
                .toTable(db);

            for (String dateStr : dates) {
                LocalDate ld = LocalDate.parse(dateStr, sdf);
                table.addRow("row " + dateStr, ld);
            }

            List<String> foundDates = new ArrayList<>();
            for (Row row : table) {
                foundDates.add(sdf.format(row.getLocalDateTime("date")));
            }

            assertEquals(dates, foundDates);
        }
    }

    void testAncientLocalDates2() throws Exception {
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        List<String> dates = List.of("1582-10-15", "1582-10-14", "1492-01-10", "1392-01-10");

        for (TestDb testDB : TestDbs.getDbs(Basename.OLD_DATES)) {
            try (Database db = testDB.openCopy()) {
                db.setDateTimeType(DateTimeType.LOCAL_DATE_TIME);

                Table t = db.getTable("Table1");

                List<String> foundDates = new ArrayList<>();
                for (Row row : t) {
                    foundDates.add(sdf.format(row.getLocalDateTime("DateField")));
                }

                assertEquals(dates, foundDates);
            }
        }

    }

    @Test
    void testZoneId() throws Exception {
        ZoneId zoneId = ZoneId.of("America/New_York");
        doTestZoneId(zoneId);

        zoneId = ZoneId.of("Australia/Sydney");
        doTestZoneId(zoneId);
    }

    private static void doTestZoneId(final ZoneId zoneId) throws Exception {
        final TimeZone tz = TimeZone.getTimeZone(zoneId);
        ColumnImpl col = new ColumnImpl(null, null, DataType.SHORT_DATE_TIME, 0, 0, 0) {
            @Override
            public TimeZone getTimeZone() {
                return tz;
            }

            @Override
            public ZoneId getZoneId() {
                return zoneId;
            }

            @Override
            public ColumnImpl.DateTimeFactory getDateTimeFactory() {
                return getDateTimeFactory(DateTimeType.LOCAL_DATE_TIME);
            }
        };

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");
        df.setTimeZone(tz);

        long startDate = df.parse("2012.01.01").getTime();
        long endDate = df.parse("2013.01.01").getTime();

        Calendar curCal = Calendar.getInstance(tz);
        curCal.setTimeInMillis(startDate);

        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss");

        while (curCal.getTimeInMillis() < endDate) {
            Date curDate = curCal.getTime();
            LocalDateTime curLdt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(curDate.getTime()), zoneId);
            LocalDateTime newLdt = ColumnImpl.ldtFromLocalDateDouble(
                col.toDateDouble(curDate));
            if (!curLdt.equals(newLdt)) {
                System.out.println("FOO " + curLdt + " " + newLdt);
                assertEquals(sdf.format(curLdt), sdf.format(newLdt));
            }
            curCal.add(Calendar.MINUTE, 30);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("io.github.spannm.jackcess.test.TestDbs#getFileformats()")
    void testWriteAndReadTemporals(FileFormat fileFormat) throws Exception {
        ZoneId zoneId = ZoneId.of("America/New_York");

        try (Database db = createMem(fileFormat)) {
            db.setZoneId(zoneId);
            db.setDateTimeType(DateTimeType.LOCAL_DATE_TIME);

            Table table = newTable("test")
                .addColumn(newColumn("name", DataType.TEXT))
                .addColumn(newColumn("date", DataType.SHORT_DATE_TIME))
                .toTable(db);

            // since jackcess does not really store millis, shave them off before
            // storing the current date/time
            long curTimeNoMillis = System.currentTimeMillis() / 1000L;
            curTimeNoMillis *= 1000L;

            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            List<Date> tmpDates = new ArrayList<>(List.of(
                df.parse("19801231 00:00:00"),
                df.parse("19930513 14:43:27"),
                df.parse("20210102 02:37:00"),
                new Date(curTimeNoMillis)));

            List<Object> objs = new ArrayList<>();
            List<LocalDateTime> expected = new ArrayList<>();
            for (Date d : tmpDates) {
                Instant inst = Instant.ofEpochMilli(d.getTime());
                objs.add(inst);
                ZonedDateTime zdt = inst.atZone(zoneId);
                objs.add(zdt);
                LocalDateTime ldt = zdt.toLocalDateTime();
                objs.add(ldt);

                for (int i = 0; i < 3; i++) {
                    expected.add(ldt);
                }
            }

            ((DatabaseImpl) db).getPageChannel().startWrite();
            try {
                for (Object o : objs) {
                    table.addRow("row " + o, o);
                }
            } finally {
                ((DatabaseImpl) db).getPageChannel().finishWrite();
            }

            List<LocalDateTime> foundDates = new ArrayList<>();
            for (Row row : table) {
                foundDates.add(row.getLocalDateTime("date"));
            }

            assertEquals(expected, foundDates);
        }
    }

}
