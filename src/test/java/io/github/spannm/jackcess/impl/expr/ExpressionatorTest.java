/*
Copyright (c) 2016 James Ahlborn

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

package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.expr.*;
import io.github.spannm.jackcess.impl.BaseEvalContext;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.converter.CsvToLocalDateTime;
import io.github.spannm.jackcess.test.converter.CsvToStringArray;
import io.github.spannm.jackcess.test.source.IntMatrixSource;
import io.github.spannm.jackcess.test.source.IntRangeSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 * @author James Ahlborn
 */
class ExpressionatorTest extends AbstractBaseTest {

    static double[] getDoublesTestData() {
        return new double[] {
            -10.3d, -9.0d, -8.234d, -7.11111d, -6.99999d, -5.5d, -4.0d, -3.4159265d, -2.84d, -1.0000002d, -1.0d,
            -0.0002013d, 0.0d, 0.9234d, 1.0d, 1.954d, 2.200032d, 3.001d, 4.9321d, 5.0d, 6.66666d, 7.396d, 8.1d,
            9.20456200d, 10.325d};
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "\"A\"; <ELiteralValue>{\"A\"}",
        "13; <ELiteralValue>{13}",
        "-42; <EUnaryOp>{- <ELiteralValue>{42}}",
        "(+37); <EParen>{(<EUnaryOp>{+ <ELiteralValue>{37}})}"
    })
    void testParseSimpleExpr1(String exprStr, String debugStr) {
        validateExpr(exprStr, debugStr);
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "EBinaryOp; +, -, *, /, \\, ^, &, Mod",
        "ECompOp; <, <=, >, >=, =, <>",
        "ELogicalOp; And, Or, Eqv, Xor, Imp",
    })
    void testParseSimpleExpr2(String opName, @ConvertWith(CsvToStringArray.class) String... ops) {
        for (String op : ops) {
            validateExpr("\"A\" " + op + " \"B\"", "<" + opName + ">{<ELiteralValue>{\"A\"} " + op + " <ELiteralValue>{\"B\"}}");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"True", "False", "Null"})
    void testParseSimpleExpr3(String constStr) {
        validateExpr(constStr, "<EConstValue>{" + constStr + "}");
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '§',
        value = {
        "[Field1]; <EObjValue>{[Field1]};",
        "[Table2].[Field3]; <EObjValue>{[Table2].[Field3]};",
        "Not \"A\"; <EUnaryOp>{Not <ELiteralValue>{\"A\"}};",
        "-[Field1]; <EUnaryOp>{- <EObjValue>{[Field1]}};",
        "\"A\" Is Null; <ENullOp>{<ELiteralValue>{\"A\"} Is Null};",
        "\"A\" In (1,2,3); <EInOp>{<ELiteralValue>{\"A\"} In (<ELiteralValue>{1},<ELiteralValue>{2},<ELiteralValue>{3})};",
        "\"A\" Not Between 3 And 7; <EBetweenOp>{<ELiteralValue>{\"A\"} Not Between <ELiteralValue>{3} And <ELiteralValue>{7}};",
        "(\"A\" Or \"B\"); <EParen>{(<ELogicalOp>{<ELiteralValue>{\"A\"} Or <ELiteralValue>{\"B\"}})};",
        "IIf(\"A\",42,False); <EFunc>{IIf(<ELiteralValue>{\"A\"},<ELiteralValue>{42},<EConstValue>{False})};",
        "\"A\" Like \"a*b\"; <ELikeOp>{<ELiteralValue>{\"A\"} Like \"a*b\"(a.*b)};",
        "' \"A\" '; <ELiteralValue>{\" \"\"A\"\" \"}; \" \"\"A\"\" \"",
        "<=1 And >=0; <ELogicalOp>{<ECompOp>{<EThisValue>{<THIS_COL>} <= <ELiteralValue>{1}} And <ECompOp>{<EThisValue>{<THIS_COL>} >= <ELiteralValue>{0}}}; <= 1 And >= 0",
    })
    void testParseSimpleExpr4(String exprStr, String debugStr, String cleanStr) {
        validateExpr(exprStr, debugStr, Objects.requireNonNullElse(cleanStr, exprStr));
    }

    @SuppressWarnings("checkstyle:LineLengthCheck")
    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "\"A\" Eqv \"B\"; <ELogicalOp>{<ELiteralValue>{\"A\"} Eqv <ELiteralValue>{\"B\"}}",
        "\"A\" Eqv \"B\" Xor \"C\"; <ELogicalOp>{<ELiteralValue>{\"A\"} Eqv <ELogicalOp>{<ELiteralValue>{\"B\"} Xor <ELiteralValue>{\"C\"}}}",
        "\"A\" Eqv \"B\" Xor \"C\" Or \"D\"; <ELogicalOp>{<ELiteralValue>{\"A\"} Eqv <ELogicalOp>{<ELiteralValue>{\"B\"} Xor <ELogicalOp>{<ELiteralValue>{\"C\"} Or <ELiteralValue>{\"D\"}}}}",
        "\"A\" Eqv \"B\" Xor \"C\" Or \"D\" And \"E\"; <ELogicalOp>{<ELiteralValue>{\"A\"} Eqv <ELogicalOp>{<ELiteralValue>{\"B\"} Xor <ELogicalOp>{<ELiteralValue>{\"C\"} Or <ELogicalOp>{<ELiteralValue>{\"D\"} And <ELiteralValue>{\"E\"}}}}}",
        "\"A\" Or \"B\" Or \"C\"; <ELogicalOp>{<ELogicalOp>{<ELiteralValue>{\"A\"} Or <ELiteralValue>{\"B\"}} Or <ELiteralValue>{\"C\"}}",
        "\"A\" & \"B\" Is Null; <ENullOp>{<EBinaryOp>{<ELiteralValue>{\"A\"} & <ELiteralValue>{\"B\"}} Is Null}",
        "\"A\" Or \"B\" Is Null; <ELogicalOp>{<ELiteralValue>{\"A\"} Or <ENullOp>{<ELiteralValue>{\"B\"} Is Null}}",
        "Not \"A\" & \"B\"; <EUnaryOp>{Not <EBinaryOp>{<ELiteralValue>{\"A\"} & <ELiteralValue>{\"B\"}}}",
        "Not \"A\" Or \"B\"; <ELogicalOp>{<EUnaryOp>{Not <ELiteralValue>{\"A\"}} Or <ELiteralValue>{\"B\"}}",
        "\"A\" + \"B\" Not Between 37 - 15 And 52 / 4; <EBetweenOp>{<EBinaryOp>{<ELiteralValue>{\"A\"} + <ELiteralValue>{\"B\"}} Not Between <EBinaryOp>{<ELiteralValue>{37} - <ELiteralValue>{15}} And <EBinaryOp>{<ELiteralValue>{52} / <ELiteralValue>{4}}}",
        "\"A\" + (\"B\" Not Between 37 - 15 And 52) / 4; <EBinaryOp>{<ELiteralValue>{\"A\"} + <EBinaryOp>{<EParen>{(<EBetweenOp>{<ELiteralValue>{\"B\"} Not Between <EBinaryOp>{<ELiteralValue>{37} - <ELiteralValue>{15}} And <ELiteralValue>{52}})} / <ELiteralValue>{4}}}",
    })
    void testOrderOfOperation(String exprStr, String debugStr) {
        validateExpr(exprStr, debugStr, exprStr);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @IntRangeSource(start = -10, end = 10, endInclusive = true)
    void testSimpleMathExpressions1(int i) {
        assertEquals(-i, eval("=-(" + i + ")"));
        assertEquals(i, eval("=+(" + i + ")"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getDoublesTestData")
    void testSimpleMathExpressions2(double d) {
        BigDecimal bd = toBD(d);
        assertEquals(bd.negate(), eval("=-(" + d + ")"));
        assertEquals(bd, eval("=+(" + d + ")"));
    }

    @ParameterizedTest(name = "[{index}] {0}, {1}")
    @IntMatrixSource(start = -10, end = 10, endInclusive = true)
    void testSimpleMathExpressions3(int i, int j) {
        assertEquals(i + j, eval("=" + i + " + " + j));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getDoublesTestData")
    void testSimpleMathExpressions4(double d1) {
        BigDecimal bd1 = toBD(d1);
        for (double j : getDoublesTestData()) {
            BigDecimal bd2 = toBD(j);
            assertEquals(toBD(bd1.add(bd2)), eval("=" + d1 + " + " + j));
            assertEquals(toBD(bd1.subtract(bd2)), eval("=" + d1 + " - " + j));
            assertEquals(toBD(bd1.multiply(bd2)), eval("=" + d1 + " * " + j));
            if (roundToLongInt(j) == 0) {
                evalFail("=" + d1 + " \\ " + j, ArithmeticException.class);
            } else {
                assertEquals(roundToLongInt(d1) / roundToLongInt(j), eval("=" + d1 + " \\ " + j));
            }
            if (roundToLongInt(j) == 0) {
                evalFail("=" + d1 + " Mod " + j, ArithmeticException.class);
            } else {
                assertEquals(roundToLongInt(d1) % roundToLongInt(j), eval("=" + d1 + " Mod " + j));
            }
            if (j == 0.0d) {
                evalFail("=" + d1 + " / " + j, ArithmeticException.class);
            } else {
                assertEquals(toBD(BuiltinOperators.divide(bd1, bd2)), eval("=" + d1 + " / " + j));
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}, {1}")
    @IntMatrixSource(start = -10, end = 10, endInclusive = true)
    void testSimpleMathExpressions5(int i, int j) {
        assertEquals(i - j, eval("=" + i + " - " + j));
        assertEquals(i * j, eval("=" + i + " * " + j));
        if (j == 0L) {
            evalFail("=" + i + " \\ " + j, ArithmeticException.class);
        } else {
            assertEquals(i / j, eval("=" + i + " \\ " + j));
        }

        if (j == 0) {
            evalFail("=" + i + " Mod " + j, ArithmeticException.class);
        } else {
            assertEquals(i % j, eval("=" + i + " Mod " + j));
        }

        if (j == 0) {
            evalFail("=" + i + " / " + j, ArithmeticException.class);
        } else {
            double result = (double) i / (double) j;
            if ((int) result == result) {
                assertEquals((int) result, eval("=" + i + " / " + j));
            } else {
                assertEquals(result, eval("=" + i + " / " + j));
            }
        }

        double result = Math.pow(i, j);
        if ((int) result == result) {
            assertEquals((int) result, eval("=" + i + " ^ " + j));
        } else {
            assertEquals(result, eval("=" + i + " ^ " + j));
        }
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "-1; ='blah'<'fuzz'",
        "0; =23>56",
        "0; =23>=56",
        "-1; =13.2<=45.8",
        "0; ='blah'='fuzz'",
        "-1; ='blah'<>'fuzz'",
        "-1; =CDbl(13.2)<=CDbl(45.8)",
        "0; ='blah' Is Null",
        "-1; ='blah' Is Not Null",
        "-1; =Null Is Null",
        "0; =Null Is Not Null",
        "-1; ='blah' Between 'a' And 'z'",
        "-1; ='blah' Between 'z' And 'a'",
        "0; ='blah' Not Between 'a' And 'z'",
        "-1; ='blah' In ('foo','bar','blah')",
        "0; ='blah' Not In ('foo','bar','blah')",
        "-1; =True Xor False",
        "-1; =True Or False",
        "-1; =False Or True",
        "0; =True Imp False",
        "0; =True Eqv False",
        "-1; =Not(True Eqv False)"
    })
    void testComparison(int expected, String exprStr) {
        TestContext tc = new TestContext();
        Expression expr = Expressionator.parse(Expressionator.Type.DEFAULT_VALUE, exprStr, null, tc);
        assertEquals(expected, expr.eval(tc));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "2003, 1, 2, 7, 0; =#01/02/2003# + #7:00:00 AM#",
        "2003, 1, 1, 17, 0; =#01/02/2003# - #7:00:00 AM#",
        "2003, 2, 8, 0, 0; =#01/02/2003# + '37'",
        "2003, 2, 8, 0, 0; ='37' + #01/02/2003#",
        "2003, 1, 2, 7, 0; =#01/02/2003 7:00:00 AM#"
    })
    void testDateArith1(@ConvertWith(CsvToLocalDateTime.class) LocalDateTime expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "2/8/2003; =CStr(#01/02/2003# + '37')",
        "9:24:00 AM; =CStr(#7:00:00 AM# + 0.1)",
        "1/2/2003 1:10:00 PM; =CStr(#01/02/2003# + #13:10:00#)"
    })
    void testDateArith2(String expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = ';', value = {
        "=37 + Null",
        "=37 - Null",
        "=37 / Null",
        "=37 * Null",
        "=37 ^ Null",
        "=37 Mod Null",
        "=37 \\ Null",
        "=-(Null)",
        "=+(Null)",
        "=Not Null",
        "=Null Or 37",
        "=37 And Null",
        "=Null And 37",
        "=37 Xor Null",
        "=37 Imp Null",
        "=Null Imp Null",
        "=37 Eqv Null",
        "=37 < Null",
        "=37 > Null",
        "=37 = Null",
        "=37 <> Null",
        "=37 <= Null",
        "=37 >= Null",
        "=37 Between Null And 54",
        "=Null In (23, Null, 45)"
    })
    void testNull1(String exprStr) {
        assertNull(eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "37; =37 & Null",
        "37; =Null & 37",
    })
    void testNull2(String expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "-1; =37 Or Null",
        "-1; =Null Imp 37",
        "0; =37 In (23, Null, 45)"
    })
    void testNull3(int expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "37; =30+7",
        "23; =30+-7",
        "23; =30-+7",
        "37; =30--7",
        "23; =30-7",

        "100; =-10^2",
        "-100; =-(10)^2",
        "-100d; =-\"10\"^2"
    })
    void testTrickyMathExpressions(double expected, String exprStr) {
        Number result = assertInstanceOf(Number.class, eval(exprStr));
        assertEquals(expected, result.doubleValue());
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "-98.9; =1.1+(-\"10\"^2)",
        "99; =-10E-1+10e+1",
        "-101; =-10E-1-10e+1"
    })
    void testTrickyMathExpressionsBd(BigDecimal expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @Test
    void testTypeCoercion() {
        assertEquals("foobar", eval("=\"foo\" + \"bar\""));

        assertEquals("12foo", eval("=12 + \"foo\""));
        assertEquals("foo12", eval("=\"foo\" + 12"));

        assertEquals(37d, eval("=\"25\" + 12"));
        assertEquals(37d, eval("=12 + \"25\""));
        assertEquals(37d, eval("=\" 25 \" + 12"));
        assertEquals(37d, eval("=\" &h1A \" + 11"));
        assertEquals(37d, eval("=\" &h1a \" + 11"));
        assertEquals(37d, eval("=\" &O32 \" + 11"));
        assertEquals(1037d, eval("=\"1,025\" + 12"));

        evalFail("=12 - \"foo\"", RuntimeException.class);
        evalFail("=\"foo\" - 12", RuntimeException.class);

        assertEquals("foo1225", eval("=\"foo\" + 12 + 25"));
        assertEquals("37foo", eval("=12 + 25 + \"foo\""));
        assertEquals("foo37", eval("=\"foo\" + (12 + 25)"));
        assertEquals("25foo12", eval("=\"25foo\" + 12"));

        assertEquals(LocalDateTime.of(2017, 1, 28, 0, 0), eval("=#1/1/2017# + 27"));
        assertEquals(128208, eval("=#1/1/2017# * 3"));
    }

    @Test
    void testLikeExpression() {
        validateExpr("Like \"[abc]*\"", "<ELikeOp>{<EThisValue>{<THIS_COL>} Like \"[abc]*\"([abc].*)}", "Like \"[abc]*\"");
        assertTrue(evalCondition("Like \"[abc]*\"", "afcd"));
        assertFalse(evalCondition("Like \"[abc]*\"", "fcd"));

        validateExpr("Like  \"[abc*\"", "<ELikeOp>{<EThisValue>{<THIS_COL>} Like \"[abc*\"((?!))}", "Like \"[abc*\"");
        assertFalse(evalCondition("Like \"[abc*\"", "afcd"));
        assertFalse(evalCondition("Like \"[abc*\"", "fcd"));
        assertTrue(evalCondition("Not Like \"[abc*\"", "fcd"));
        assertFalse(evalCondition("Like \"[abc*\"", ""));
    }

    @Test
    void testLiteralDefaultValue() {
        assertEquals("-28 blah ", eval("=CDbl(9)-37 & \" blah \"", Value.Type.STRING));
        assertEquals("CDbl(9)-37 & \" blah \"", eval("CDbl(9)-37 & \" blah \"", Value.Type.STRING));

        assertEquals(-28d, eval("=CDbl(9)-37", Value.Type.DOUBLE));
        assertEquals(-28d, eval("CDbl(9)-37", Value.Type.DOUBLE));
    }

    @Test
    void testParseSomeExprs() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/test_exprs.txt"));

        TestContext tc = new TestContext() {
            @Override
            public Value getThisColumnValue() {
                return ValueSupport.toValue(23.0);
            }

            @Override
            public Value getIdentifierValue(Identifier identifier) {
                return ValueSupport.toValue(23.0);
            }
        };

        String line = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(";", 3);
            Expressionator.Type type = Expressionator.Type.valueOf(parts[0]);
            DataType dType =
                "null".equals(parts[1]) ? null : DataType.valueOf(parts[1]);
            String exprStr = parts[2];

            Value.Type resultType = dType != null ? BaseEvalContext.toValueType(dType) : null;

            Expression expr = Expressionator.parse(
                type, exprStr, resultType, tc);

            expr.eval(tc);
        }

        br.close();
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "'';empty",
        "=;found?",
        "=(34 + 5;closing",
        "=(34 + );found?",
        "=(34 + [A].[B].[C].[D]);object reference",
        "=34 + 5,;delimiter",
        "=Foo();find function",
        "=(/37);left expression",
        "=(>37);left expression",
        "=(And 37);left expression",
        "=37 In 42;'In' expression",
        "=37 Between 42;'Between' expression",
        "=(3 + 5) Rnd();multiple expressions"
    })
    void testInvalidExpression(String exprStr, String msgStr) {
        ParseException ex = assertThrows(ParseException.class, () -> eval(exprStr));
        assertTrue(ex.getMessage().contains(msgStr));
    }

    private static void validateExpr(String exprStr, String debugStr) {
        validateExpr(exprStr, debugStr, exprStr);
    }

    private static void validateExpr(String exprStr, String debugStr, String cleanStr) {
        TestContext ctx = new TestContext();
        Expression expr = Expressionator.parse(Expressionator.Type.FIELD_VALIDATOR, exprStr, null, ctx);
        String foundDebugStr = expr.toDebugString(ctx);
        if (foundDebugStr.startsWith("<EImplicitCompOp>")) {
            assertEquals("<EImplicitCompOp>{<EThisValue>{<THIS_COL>} = " + debugStr + "}", foundDebugStr);
        } else {
            assertEquals(debugStr, foundDebugStr);
        }
        assertEquals(cleanStr, expr.toCleanString(ctx));
        assertEquals(exprStr, expr.toRawString());
    }

    static Object eval(String exprStr) {
        return eval(exprStr, null);
    }

    static Object eval(String exprStr, Value.Type resultType) {
        TestContext tc = new TestContext();
        Expression expr = Expressionator.parse(Expressionator.Type.DEFAULT_VALUE, exprStr, resultType, tc);
        return expr.eval(tc);
    }

    private static void evalFail(String exprStr, Class<? extends Exception> failure) {
        TestContext tc = new TestContext();
        Expression expr = Expressionator.parse(Expressionator.Type.DEFAULT_VALUE, exprStr, null, tc);
        assertThrows(failure, () -> expr.eval(tc));
    }

    private static Boolean evalCondition(String exprStr, String thisVal) {
        TestContext tc = new TestContext(ValueSupport.toValue(thisVal));
        Expression expr = Expressionator.parse(Expressionator.Type.FIELD_VALIDATOR, exprStr, null, tc);
        return (Boolean) expr.eval(tc);
    }

    static int roundToLongInt(double d) {
        return BigDecimal.valueOf(d).setScale(0, NumberFormatter.ROUND_MODE).intValueExact();
    }

    static BigDecimal toBD(double d) {
        return toBD(BigDecimal.valueOf(d));
    }

    static BigDecimal toBD(BigDecimal bd) {
        return ValueSupport.normalize(bd);
    }

    private static class TestContext implements Expressionator.ParseContext, EvalContext {
        private final Value         _thisVal;
        private final RandomContext _rndCtx   = new RandomContext();
        private final Bindings      _bindings = new SimpleBindings();

        private TestContext() {
            this(null);
        }

        private TestContext(Value thisVal) {
            _thisVal = thisVal;
        }

        @Override
        public Value.Type getResultType() {
            return null;
        }

        @Override
        public TemporalConfig getTemporalConfig() {
            return TemporalConfig.US_TEMPORAL_CONFIG;
        }

        @Override
        public DateTimeFormatter createDateFormatter(String formatStr) {
            return DateTimeFormatter.ofPattern(
                formatStr, TemporalConfig.US_TEMPORAL_CONFIG.getLocale());
        }

        @Override
        public ZoneId getZoneId() {
            return TestUtil.TEST_TZ.toZoneId();
        }

        @Override
        public NumericConfig getNumericConfig() {
            return NumericConfig.US_NUMERIC_CONFIG;
        }

        @Override
        public DecimalFormat createDecimalFormat(String formatStr) {
            return new DecimalFormat(
                formatStr, NumericConfig.US_NUMERIC_CONFIG.getDecimalFormatSymbols());
        }

        @Override
        public FunctionLookup getFunctionLookup() {
            return DefaultFunctions.LOOKUP;
        }

        @Override
        public Value getThisColumnValue() {
            if (_thisVal == null) {
                throw new UnsupportedOperationException();
            }
            return _thisVal;
        }

        @Override
        public Value getIdentifierValue(Identifier identifier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getRandom(Integer seed) {
            return _rndCtx.getRandom(seed);
        }

        @Override
        public Bindings getBindings() {
            return _bindings;
        }

        @Override
        public Object get(String key) {
            return _bindings.get(key);
        }

        @Override
        public void put(String key, Object value) {
            _bindings.put(key, value);
        }
    }

}
