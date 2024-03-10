/*
Copyright (c) 2017 James Ahlborn

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

package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.DatabaseBuilder.*;
import static io.github.spannm.jackcess.test.TestUtil.assertCursor;
import static io.github.spannm.jackcess.test.TestUtil.create;
import static io.github.spannm.jackcess.test.TestUtil.createExpectedRow;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author James Ahlborn
 */
class BigIntTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource()
    void testBigInt(FileFormat fileFormat) throws Exception {
        JetFormat format = DatabaseImpl.getFileFormatDetails(fileFormat)
            .getFormat();

        if (!format.isSupportedDataType(DataType.BIG_INT)) {
            return;
        }

        try (Database db = create(fileFormat)) {
            Table t = newTable("Test")
                .addColumn(newColumn("id", DataType.LONG)
                    .withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.TEXT))
                .addColumn(newColumn("num1", DataType.BIG_INT))
                .addIndex(newIndex("idx").withColumns("num1"))
                .toTable(db);

            long[] vals = new long[] {0L, -10L, 3844L, -45309590834L, 50392084913L, 65000L, -6489273L};

            List<Map<String, Object>> expectedTable =
                new ArrayList<>();

            int idx = 1;
            for (long lng : vals) {
                t.addRow(Column.AUTO_NUMBER, "" + lng, lng);

                expectedTable.add(createExpectedRow(
                    "id", idx++,
                    "data1", "" + lng,
                    "num1", lng));
            }

            expectedTable.sort((r1, r2) -> {
                Long l1 = (Long) r1.get("num1");
                Long l2 = (Long) r2.get("num1");
                return l1.compareTo(l2);
            });

            Cursor c = t.newCursor().withIndexByName("idx").toIndexCursor();

            assertCursor(expectedTable, c);
        }
    }
}
