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
import static io.github.spannm.jackcess.test.TestUtil.*;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.expr.*;
import io.github.spannm.jackcess.impl.expr.DefaultFunctions;
import io.github.spannm.jackcess.impl.expr.FunctionSupport;
import io.github.spannm.jackcess.impl.expr.ValueSupport;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.script.Bindings;
import javax.script.SimpleBindings;

class PropertyExpressionTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testDefaultValue(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            db.setEvaluateExpressions(true);

            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.TEXT)
                    .withProperty(PropertyMap.DEFAULT_VALUE_PROP,
                        "=\"FOO \" & \"BAR\""))
                .addColumn(newColumn("data2", DataType.LONG)
                    .withProperty(PropertyMap.DEFAULT_VALUE_PROP,
                        "37"))
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, null, 13);
            t.addRow(Column.AUTO_NUMBER, "blah", null);

            setProp(t, "data1", PropertyMap.DEFAULT_VALUE_PROP, null);
            setProp(t, "data2", PropertyMap.DEFAULT_VALUE_PROP, "42");

            t.addRow(Column.AUTO_NUMBER, null, null);

            List<Row> expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 1,
                        "data1", "FOO BAR",
                        "data2", 13),
                    createExpectedRow(
                        "id", 2,
                        "data1", "blah",
                        "data2", 37),
                    createExpectedRow(
                        "id", 3,
                        "data1", null,
                        "data2", 42));

            assertTable(expectedRows, t);

            setProp(t, "data2", PropertyMap.REQUIRED_PROP, true);

            t.addRow(Column.AUTO_NUMBER, "blah", 13);
            t.addRow(Column.AUTO_NUMBER, "blah", null);

            expectedRows = new ArrayList<>(expectedRows);
            expectedRows.add(
                createExpectedRow(
                    "id", 4,
                    "data1", "blah",
                    "data2", 13));
            expectedRows.add(
                createExpectedRow(
                    "id", 5,
                    "data1", "blah",
                    "data2", 42));

            assertTable(expectedRows, t);
        }
    }

    @Test
    void testCalculatedValue() throws IOException {
        try (Database db = createDbMem(FileFormat.V2016)) {
            db.setEvaluateExpressions(true);

            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(newColumn("c1", DataType.LONG)
                    .withCalculatedInfo("[c2]+[c3]"))
                .addColumn(newColumn("c2", DataType.LONG)
                    .withCalculatedInfo("[c3]*5"))
                .addColumn(newColumn("c3", DataType.LONG)
                    .withCalculatedInfo("[c4]-6"))
                .addColumn(newColumn("c4", DataType.LONG))
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, null, null, null, 16);

            setProp(t, "c1", PropertyMap.EXPRESSION_PROP, "[c4]+2");
            setProp(t, "c2", PropertyMap.EXPRESSION_PROP, "[c1]+[c3]");
            setProp(t, "c3", PropertyMap.EXPRESSION_PROP, "[c1]*7");

            t.addRow(Column.AUTO_NUMBER, null, null, null, 7);

            List<Row> expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 1,
                        "c1", 60,
                        "c2", 50,
                        "c3", 10,
                        "c4", 16),
                    createExpectedRow(
                        "id", 2,
                        "c1", 9,
                        "c2", 72,
                        "c3", 63,
                        "c4", 7));

            assertTable(expectedRows, t);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testColumnValidator(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            db.setEvaluateExpressions(true);

            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.LONG)
                    .withProperty(PropertyMap.VALIDATION_RULE_PROP,
                        ">37"))
                .addColumn(newColumn("data2", DataType.LONG)
                    .withProperty(PropertyMap.VALIDATION_RULE_PROP,
                        "between 7 and 10")
                    .withProperty(PropertyMap.VALIDATION_TEXT_PROP,
                        "You failed"))
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, 42, 8);

            assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, 42, 20));

            assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, 3, 8));

            t.addRow(Column.AUTO_NUMBER, 54, 9);

            setProp(t, "data1", PropertyMap.VALIDATION_RULE_PROP, null);
            setProp(t, "data2", PropertyMap.VALIDATION_RULE_PROP, "<100");
            setProp(t, "data2", PropertyMap.VALIDATION_TEXT_PROP, "Too big");

            InvalidValueException ex = assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, 42, 200));
            assertTrue(ex.getMessage().contains("Too big"));

            t.addRow(Column.AUTO_NUMBER, 1, 9);

            List<Row> expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 1,
                        "data1", 42,
                        "data2", 8),
                    createExpectedRow(
                        "id", 2,
                        "data1", 54,
                        "data2", 9),
                    createExpectedRow(
                        "id", 3,
                        "data1", 1,
                        "data2", 9));

            assertTable(expectedRows, t);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testRowValidator(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            db.setEvaluateExpressions(true);

            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.LONG))
                .addColumn(newColumn("data2", DataType.LONG))
                .putProperty(PropertyMap.VALIDATION_RULE_PROP,
                    "([data1] > 10) and ([data2] < 100)")
                .putProperty(PropertyMap.VALIDATION_TEXT_PROP,
                    "You failed")
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, 42, 8);

            assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, 1, 20));

            t.addRow(Column.AUTO_NUMBER, 54, 9);

            setTableProp(t, PropertyMap.VALIDATION_RULE_PROP, "[data2]<100");
            setTableProp(t, PropertyMap.VALIDATION_TEXT_PROP, "Too big");

            assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, 42, 200));

            t.addRow(Column.AUTO_NUMBER, 1, 9);

            List<Row> expectedRows =
                createExpectedTable(
                    createExpectedRow(
                        "id", 1,
                        "data1", 42,
                        "data2", 8),
                    createExpectedRow(
                        "id", 2,
                        "data1", 54,
                        "data2", 9),
                    createExpectedRow(
                        "id", 3,
                        "data1", 1,
                        "data2", 9));

            assertTable(expectedRows, t);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCustomEvalConfig(FileFormat fileFormat) throws IOException {
        TemporalConfig tempConf = new TemporalConfig("[uuuu/]M/d",
            "uuuu-MMM-d",
            "hh.mm.ss a",
            "HH.mm.ss", '/', '.',
            Locale.US);

        FunctionLookup lookup = name -> {
            if ("FooFunc".equalsIgnoreCase(name)) {
                return FOO;
            }
            return DefaultFunctions.LOOKUP.getFunction(name);
        };

        Bindings bindings = new SimpleBindings();
        bindings.put("someKey", "someVal");

        try (Database db = createDbMem(fileFormat)) {
            EvalConfig ec = db.getEvalConfig();
            ec.setTemporalConfig(tempConf);
            ec.setFunctionLookup(lookup);
            ec.setBindings(bindings);

            db.setEvaluateExpressions(true);

            Table t = newTable("test")
                .addColumn(newColumn("id", DataType.LONG).withAutoNumber(true))
                .addColumn(newColumn("data1", DataType.TEXT)
                    .withProperty(PropertyMap.DEFAULT_VALUE_PROP,
                        "=FooFunc()"))
                .addColumn(newColumn("data2", DataType.TEXT)
                    .withProperty(PropertyMap.DEFAULT_VALUE_PROP,
                        "=Date()"))
                .addColumn(newColumn("data3", DataType.TEXT)
                    .withProperty(PropertyMap.DEFAULT_VALUE_PROP,
                        "=Time()"))
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, null, null);

            Row row = t.iterator().next();

            assertEquals(1, row.get("id"));
            assertEquals("FOO_someVal", row.get("data1"));
            assertTrue(((String) row.get("data2"))
                .matches("\\d{4}/\\d{1,2}/\\d{1,2}"));
            assertTrue(((String) row.get("data3"))
                .matches("\\d{2}.\\d{2}.\\d{2} (AM|PM)"));
        }
    }

    private static void setProp(Table t, String colName, String propName,
        Object propVal) throws IOException {
        PropertyMap props = t.getColumn(colName).getProperties();
        if (propVal != null) {
            props.put(propName, propVal);
        } else {
            props.remove(propName);
        }
        props.save();
    }

    private static void setTableProp(Table t, String propName,
        String propVal) throws IOException {
        PropertyMap props = t.getProperties();
        if (propVal != null) {
            props.put(propName, propVal);
        } else {
            props.remove(propName);
        }
        props.save();
    }

    private static final Function FOO = new FunctionSupport.Func0("FooFunc") {

        @Override
        protected Value eval0(EvalContext ctx) {
            Object val = ctx.get("someKey");
            return ValueSupport.toValue("FOO_" + val);
        }
    };
}
