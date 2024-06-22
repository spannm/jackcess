/*
Copyright (c) 2020 James Ahlborn

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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PatternColumnPredicateTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testRegexPredicate(FileFormat fileFormat) throws Exception {
        try (Database db = createTestDb(fileFormat)) {
            Table t = db.getTable("Test");

            assertEquals(List.of("Foo", "some row", "aNoThEr row", "nonsense"), findRowsByPattern(t, PatternColumnPredicate.forJavaRegex(".*o.*")));

            assertEquals(List.of("Bar", "0102", "FOO", "BAR", "67", "bunch_13_data", "42 is the ANSWER", "[try] matching t.h+i}s"),
                findRowsByPattern(t, PatternColumnPredicate.forJavaRegex(".*o.*").negate()));

            assertEquals(List.of("Foo", "some row", "FOO", "aNoThEr row", "nonsense"), findRowsByPattern(t, PatternColumnPredicate.forAccessLike("*o*")));

            assertEquals(List.of("0102", "67", "bunch_13_data", "42 is the ANSWER"), findRowsByPattern(t, PatternColumnPredicate.forAccessLike("*##*")));

            assertEquals(List.of("42 is the ANSWER"), findRowsByPattern(t, PatternColumnPredicate.forAccessLike("## *")));

            assertEquals(List.of("Foo"), findRowsByPattern(t, PatternColumnPredicate.forSqlLike("F_o")));

            assertEquals(List.of("Foo", "FOO"), findRowsByPattern(t, PatternColumnPredicate.forSqlLike("F_o", true)));

            assertEquals(List.of("[try] matching t.h+i}s"), findRowsByPattern(t, PatternColumnPredicate.forSqlLike("[try] % t.h+i}s")));

            assertEquals(List.of("bunch_13_data"), findRowsByPattern(t, PatternColumnPredicate.forSqlLike("bunch\\_%\\_data")));
        }
    }

    private static List<String> findRowsByPattern(
        Table t, Predicate<Object> pred) {
        return t.getDefaultCursor().newIterable()
            .withMatchPattern("data", pred)
            .stream()
            .map(r -> r.getString("data"))
            .collect(Collectors.toList());
    }

    private Database createTestDb(FileFormat fileFormat) throws Exception {
        Database db = createDbMem(fileFormat);

        Table table = new TableBuilder("Test")
            .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
            .addColumn(new ColumnBuilder("data", DataType.TEXT))
            .withPrimaryKey("id")
            .toTable(db);

        table.addRow(Column.AUTO_NUMBER, "Foo");
        table.addRow(Column.AUTO_NUMBER, "some row");
        table.addRow(Column.AUTO_NUMBER, "Bar");
        table.addRow(Column.AUTO_NUMBER, "0102");
        table.addRow(Column.AUTO_NUMBER, "FOO");
        table.addRow(Column.AUTO_NUMBER, "BAR");
        table.addRow(Column.AUTO_NUMBER, "67");
        table.addRow(Column.AUTO_NUMBER, "aNoThEr row");
        table.addRow(Column.AUTO_NUMBER, "bunch_13_data");
        table.addRow(Column.AUTO_NUMBER, "42 is the ANSWER");
        table.addRow(Column.AUTO_NUMBER, "[try] matching t.h+i}s");
        table.addRow(Column.AUTO_NUMBER, "nonsense");

        return db;
    }
}
