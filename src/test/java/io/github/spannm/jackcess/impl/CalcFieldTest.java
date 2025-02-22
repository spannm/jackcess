package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.CALC_FIELD;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CalcFieldTest extends AbstractBaseTest {

    @Test
    void testColumnBuilder() {
        ColumnBuilder cb = new ColumnBuilder("calc_data", DataType.TEXT)
            .withCalculatedInfo("[id] & \"_\" & [data]");

        assertThrows(IllegalArgumentException.class, () -> cb.validate(JetFormat.VERSION_12));

        cb.validate(JetFormat.VERSION_14);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCreateCalcField(FileFormat fileFormat) throws IOException {
        JetFormat format = DatabaseImpl.getFileFormatDetails(fileFormat).getFormat();
        if (!format.isSupportedCalculatedDataType(DataType.TEXT)) {
            return;
        }

        try (Database db = createDbMem(fileFormat)) {
            db.setEvaluateExpressions(false);

            Table t = new TableBuilder("Test")
                .putProperty("awesome_table", true)
                .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
                .addColumn(new ColumnBuilder("data", DataType.TEXT))
                .addColumn(new ColumnBuilder("calc_text", DataType.TEXT).withCalculatedInfo("[id] & \"_\" & [data]"))
                .addColumn(new ColumnBuilder("calc_memo", DataType.MEMO).withCalculatedInfo("[id] & \"_\" & [data]"))
                .addColumn(new ColumnBuilder("calc_bool", DataType.BOOLEAN).withCalculatedInfo("[id] > 0"))
                .addColumn(new ColumnBuilder("calc_long", DataType.LONG).withCalculatedInfo("[id] + 1"))
                .addColumn(new ColumnBuilder("calc_numeric", DataType.NUMERIC).withCalculatedInfo("[id] / 0.03"))
                .toTable(db);

            Column col = t.getColumn("calc_text");
            assertTrue(col.isCalculated());
            assertEquals("[id] & \"_\" & [data]", col.getProperties().getValue(
                PropertyMap.EXPRESSION_PROP));
            assertEquals(DataType.TEXT.getValue(),
                col.getProperties().getValue(
                    PropertyMap.RESULT_TYPE_PROP));

            String longStr = TestUtil.createString(1000);
            BigDecimal bd1 = new BigDecimal("-1234.5678");
            BigDecimal bd2 = new BigDecimal("0.0234");

            t.addRow(Column.AUTO_NUMBER, "foo", "1_foo", longStr, true, 2, bd1);
            t.addRow(Column.AUTO_NUMBER, "bar", "2_bar", longStr, false, -37, bd2);
            t.addRow(Column.AUTO_NUMBER, "", "", "", false, 0, BigDecimal.ZERO);
            t.addRow(Column.AUTO_NUMBER, null, null, null, null, null, null);

            List<? extends Map<String, Object>> expectedRows = TestUtil.createExpectedTable(
                TestUtil.createExpectedRow(
                    "id", 1,
                    "data", "foo",
                    "calc_text", "1_foo",
                    "calc_memo", longStr,
                    "calc_bool", true,
                    "calc_long", 2,
                    "calc_numeric", bd1),
                TestUtil.createExpectedRow(
                    "id", 2,
                    "data", "bar",
                    "calc_text", "2_bar",
                    "calc_memo", longStr,
                    "calc_bool", false,
                    "calc_long", -37,
                    "calc_numeric", bd2),
                TestUtil.createExpectedRow(
                    "id", 3,
                    "data", "",
                    "calc_text", "",
                    "calc_memo", "",
                    "calc_bool", false,
                    "calc_long", 0,
                    "calc_numeric", BigDecimal.ZERO),
                TestUtil.createExpectedRow(
                    "id", 4,
                    "data", null,
                    "calc_text", null,
                    "calc_memo", null,
                    "calc_bool", null,
                    "calc_long", null,
                    "calc_numeric", null));

            TestUtil.assertTable(expectedRows, t);
        }
    }

    @SuppressWarnings("checkstyle:LineLengthCheck")
    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(CALC_FIELD)
    void testReadCalcFields(TestDb testDb) throws IOException {
        try (Database db = testDb.open()) {
            List<String> actual = db.getTable("Table1").stream().map(r -> r.entrySet().toString()).collect(Collectors.toList());

            List<String> expected = List.of(
                "[ID=1, FirstName=Bruce, LastName=Wayne, LastFirst=Wayne, Bruce, City=Gotham, LastFirstLen=12, Salary=1000000.0000, MonthlySalary=83333.3333, IsRich=true, AllNames=Wayne, Bruce=Wayne, Bruce, WeeklySalary=19230.7692307692, SalaryTest=1000000.0000, BoolTest=true, Popularity=50.325000, DecimalTest=50.325000, FloatTest=2583.2092, BigNumTest=56505085819.424791296572280180]",
                "[ID=2, FirstName=Bart, LastName=Simpson, LastFirst=Simpson, Bart, City=Springfield, LastFirstLen=13, Salary=-1.0000, MonthlySalary=-0.0833, IsRich=false, AllNames=Simpson, Bart=Simpson, Bart, WeeklySalary=-0.0192307692307692, SalaryTest=-1.0000, BoolTest=true, Popularity=-36.222200, DecimalTest=-36.222200, FloatTest=0.0035889593, BigNumTest=-0.0784734499180612994241100748]",
                "[ID=3, FirstName=John, LastName=Doe, LastFirst=Doe, John, City=Nowhere, LastFirstLen=9, Salary=0.0000, MonthlySalary=0.0000, IsRich=false, AllNames=Doe, John=Doe, John, WeeklySalary=0, SalaryTest=0.0000, BoolTest=true, Popularity=0.012300, DecimalTest=0.012300, FloatTest=0.0, BigNumTest=0E-8]",
                "[ID=4, FirstName=Test, LastName=User, LastFirst=User, Test, City=Hockessin, LastFirstLen=10, Salary=100.0000, MonthlySalary=8.3333, IsRich=false, AllNames=User, Test=User, Test, WeeklySalary=1.92307692307692, SalaryTest=100.0000, BoolTest=true, Popularity=102030405060.654321, DecimalTest=102030405060.654321, FloatTest=1.27413E-10, BigNumTest=2.787019289824216980830E-7]");

            assertEquals(expected, actual);
        }
    }

}
