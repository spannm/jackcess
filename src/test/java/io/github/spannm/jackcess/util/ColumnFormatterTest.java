/*
Copyright (c) 2019 James Ahlborn

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

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author James Ahlborn
 */
class ColumnFormatterTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testFormat(FileFormat fileFormat) throws Exception {
        try (Database db = createDbMem(fileFormat)) {
            db.setEvaluateExpressions(true);

            Table t = new TableBuilder("test")
                .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
                .addColumn(new ColumnBuilder("data1", DataType.TEXT)
                    .withProperty(PropertyMap.FORMAT_PROP,
                        ">@@\\x\\x"))
                .addColumn(new ColumnBuilder("data2", DataType.LONG)
                    .withProperty(PropertyMap.FORMAT_PROP,
                        "#.#E+0"))
                .addColumn(new ColumnBuilder("data3", DataType.MONEY)
                    .withProperty(PropertyMap.FORMAT_PROP,
                        "Currency"))
                .toTable(db);

            ColumnFormatter d1Fmt = new ColumnFormatter(t.getColumn("data1"));
            ColumnFormatter d2Fmt = new ColumnFormatter(t.getColumn("data2"));
            ColumnFormatter d3Fmt = new ColumnFormatter(t.getColumn("data3"));

            t.addRow(Column.AUTO_NUMBER, "foobar", 37, "0.03");
            t.addRow(Column.AUTO_NUMBER, "37", 4500, 4500);
            t.addRow(Column.AUTO_NUMBER, "foobarbaz", -37, "-37.13");
            t.addRow(Column.AUTO_NUMBER, null, null, null);

            List<String> found = new ArrayList<>();
            for (Row r : t) {
                found.add(d1Fmt.getRowValue(r));
                found.add(d2Fmt.getRowValue(r));
                found.add(d3Fmt.getRowValue(r));
            }

            assertEquals(List.of(
                "FOxxOBAR", "3.7E+1", "$0.03",
                "37xx", "4.5E+3", "$4,500.00",
                "FOxxOBARBAZ", "-3.7E+1", "($37.13)",
                "", "", ""),
                found);

            d1Fmt.setFormatString("Scientific");
            d2Fmt.setFormatString(null);
            d3Fmt.setFormatString("General Date");

            assertEquals("Scientific", t.getColumn("data1").getProperties()
                .getValue(PropertyMap.FORMAT_PROP));
            assertEquals("General Date", t.getColumn("data3").getProperties()
                .getValue(PropertyMap.FORMAT_PROP));

            found = new ArrayList<>();
            for (Row r : t) {
                found.add(d1Fmt.getRowValue(r));
                found.add(d2Fmt.getRowValue(r));
                found.add(d3Fmt.getRowValue(r));
            }

            assertEquals(List.of(
                "foobar", "37", "12:43:12 AM",
                "3.70E+1", "4500", "4/26/1912",
                "foobarbaz", "-37", "11/23/1899 3:07:12 AM",
                "", "", ""),
                found);
        }
    }
}
