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

import static io.github.spannm.jackcess.impl.expr.ExpressionatorTest.eval;
import static io.github.spannm.jackcess.impl.expr.ExpressionatorTest.toBD;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.converter.CsvToLocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author James Ahlborn
 */
class DefaultFunctionsTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', quoteCharacter = '\"', value = {
        "IIf(10 > 1, \"foo\", \"bar\"); foo",
        "IIf(10 < 1, \"foo\", \"bar\"); bar",
        "Chr(102); f",
        "ChrW(9786); ☺",
        "Hex(9786); 263A",
        "Nz(\"blah\"); blah",
        "Nz(Null); \"\"",
        "Nz(\"blah\",\"FOO\"); blah",
        "Nz(Null,\"FOO\"); FOO",
        "Oct(9786); 23072",
        "Str(9786); \" 9786\"",
        "Str(-42); -42",
        "Str$(-42); -42",

        "CStr(9786); 9786",
        "CStr(-42); -42",
        "TypeName(Null); Null",
        "TypeName('blah'); String",
        "TypeName(#01/02/2003#); Date",
        "TypeName(42); Long",
        "TypeName(CDbl(42)); Double",
        "TypeName(42.3); Decimal",

        "UCase(\"fOoO\"); FOOO",
        "LCase(\"fOoO\"); fooo",

        "Left(\"blah\", 2); bl",
        "Left(\"blah\", 0); \"\"",
        "Left(\"blah\", 17); blah",
        "Mid(\"blah\", 2, 2); la",

        "Right(\"blah\", 2); ah",
        "Right(\"blah\", 0); \"\"",
        "Right(\"blah\", 17); blah",

        "LTrim(\"  blah  \"); \"blah  \"",
        "RTrim(\"  blah  \"); \"  blah\"",
        "Trim(\"  blah  \"); blah",
        "Space(3); \"   \"",
        "String(3,'d'); ddd",

        "StrConv('foo', 1); FOO",
        "StrConv('foo', 2); foo",
        "StrConv('FOO', 2); foo",
        "StrConv('FOO bar', 3); Foo Bar",

        "StrReverse('blah'); halb",

        "Choose(1,'foo','bar','blah'); foo",

        "Choose(3,'foo','bar','blah'); blah",
        "Switch(False,'foo', True, 'bar', True, 'blah'); bar",
        "Switch(False,'foo', False, 'bar', True, 'blah'); blah",
        "Replace('foo','o','a'); faa",
        "Replace('fOo','o','a'); faa",
        "Replace('foo','o','a',2); aa",
        "Replace('foo','o','a',2,0); oo",
        "Replace('foo','o','a',4); \"\"",
        "Replace('foo','','a'); foo",
        "Replace('foo','','a',3); o",
        "Replace('fooboooo','OO','ahha'); fahhabahhaahha",
        "Replace('fooboooo','OO','ahha',1,1); fahhaboooo",
        "Replace('fooboooo','OO','ahha',1,1,0); fooboooo",
        "Replace('fooboooo','OO','ahha',2); ahhabahhaahha",
        "Replace('fooboooo','OO','ahha',3); obahhaahha",
        "Replace('fooboooo','OO',''); fb",
        "Replace('','o','a'); \"\"",
        "Replace('foo','foobar','a'); foo",
        "FormatNumber(12345); 12,345.00",
        "FormatNumber(0.12345); 0.12",
        "FormatNumber(12.345); 12.34",
        "FormatNumber(-12345); -12,345.00",
        "FormatNumber(-0.12345); -0.12",
        "FormatNumber(-12.345); -12.34",
        "FormatNumber(12345,3); 12,345.000",
        "FormatNumber(0.12345,3); 0.123",
        "FormatNumber(12.345,3); 12.345",
        "FormatNumber(12345,0); 12,345",
        "FormatNumber(0.12345,0); 0",
        "FormatNumber(12.345,0); 12",
        "FormatNumber(0.12345,3,True); 0.123",
        "FormatNumber(0.12345,3,False); .123",
        "FormatNumber(-0.12345,3,True); -0.123",
        "FormatNumber(-0.12345,3,False); -.123",
        "FormatNumber(-12.345,-1,True,False); -12.34",
        "FormatNumber(-12.345,-1,True,True); (12.34)",
        "FormatNumber(-12.345,0,True,True); (12)",
        "FormatNumber(12345,-1,-2,-2,True); 12,345.00",
        "FormatNumber(12345,-1,-2,-2,False); 12345.00",

        "FormatPercent(12345); 1,234,500.00%",
        "FormatPercent(-12.345,-1,True,True); (1,234.50%)",
        "FormatPercent(0.345,0,True,True); 34%",
        "FormatPercent(-0.0012345,3,False); -.123%",

        "FormatCurrency(12345); $12,345.00",
        "FormatCurrency(-12345); ($12,345.00)",
        "FormatCurrency(-12.345,-1,True,False); -$12.34",
        "FormatCurrency(12.345,0,True,True); $12",
        "FormatCurrency(-0.12345,3,False); ($.123)",

        "FormatDateTime(#1/1/1973 1:37:25 PM#); 1/1/1973 1:37:25 PM",
        "FormatDateTime(#1:37:25 PM#,0); 1:37:25 PM",
        "FormatDateTime(#1/1/1973#,0); 1/1/1973",
        "FormatDateTime(#1/1/1973 1:37:25 PM#,1); Monday, January 01, 1973",
        "FormatDateTime(#1/1/1973 1:37:25 PM#,2); 1/1/1973",
        "FormatDateTime(#1/1/1973 1:37:25 PM#,3); 1:37:25 PM",
        "FormatDateTime(#1/1/1973 1:37:25 PM#,4); 13:37"
    })
    void testFuncsString(String exprStr, String expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @Test
    void testFuncsQuoting() {
        assertEquals(" FOO \" BAR ", eval("=UCase(\" foo \"\" bar \")"));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "Asc(\"foo\"); 102",
        "AscW(\"☺\"); 9786",
        "CBool(\"1\"); -1",
        "CByte(\"13\"); 13",
        "CByte(\"13.7\"); 14",
        "CInt(\"513\"); 513",
        "CInt(\"513.7\"); 514",
        "CLng(\"345513\"); 345513",
        "CLng(\"345513.7\"); 345514",
        "IsNull(Null); -1",
        "IsNull(13); 0",
        "IsDate(#01/02/2003#); -1",
        "IsDate('foo'); 0",
        "IsDate('200'); 0",

        "IsNumeric(Null); 0",
        "IsNumeric('foo'); 0",
        "IsNumeric(#01/02/2003#); 0",
        "IsNumeric('01/02/2003'); 0",
        "IsNumeric(37); -1",
        "IsNumeric(' 37 '); -1",
        "IsNumeric(' -37.5e2 '); -1",
        "IsNumeric(' &H37 '); -1",
        "IsNumeric(' &H37foo '); 0",
        "IsNumeric(' &o39 '); 0",
        "IsNumeric(' &o36 '); -1",
        "IsNumeric(' &o36.1 '); 0",

        "VarType(Null); 1",
        "VarType('blah'); 8",
        "VarType(#01/02/2003#); 7",
        "VarType(42); 3",
        "VarType(CDbl(42)); 5",
        "VarType(42.3); 14",

        "InStr('AFOOBAR', 'FOO'); 2",
        "InStr('AFOOBAR', 'foo'); 2",
        "InStr(1, 'AFOOBAR', 'foo'); 2",
        "InStr(1, 'AFOOBAR', 'foo', 0); 0",
        "InStr(1, 'AFOOBAR', 'foo', 1); 2",
        "InStr(1, 'AFOOBAR', 'FOO', 0); 2",
        "InStr(2, 'AFOOBAR', 'FOO'); 2",
        "InStr(3, 'AFOOBAR', 'FOO'); 0",
        "InStr(17, 'AFOOBAR', 'FOO'); 0",
        "InStr(1, 'AFOOBARFOOBAR', 'FOO'); 2",
        "InStr(3, 'AFOOBARFOOBAR', 'FOO'); 8",

        "InStrRev('AFOOBAR', 'FOO'); 2",
        "InStrRev('AFOOBAR', 'foo'); 2",
        "InStrRev('AFOOBAR', 'foo', -1); 2",
        "InStrRev('AFOOBAR', 'foo', -1, 0); 0",
        "InStrRev('AFOOBAR', 'foo', -1, 1); 2",
        "InStrRev('AFOOBAR', 'FOO', -1, 0); 2",
        "InStrRev('AFOOBAR', 'FOO', 4); 2",
        "InStrRev('AFOOBAR', 'FOO', 3); 0",
        "InStrRev('AFOOBAR', 'FOO', 17); 2",
        "InStrRev('AFOOBARFOOBAR', 'FOO', 9); 2",
        "InStrRev('AFOOBARFOOBAR', 'FOO', 10); 8",

        "StrComp('FOO', 'bar'); 1",
        "StrComp('bar', 'FOO'); -1",
        "StrComp('FOO', 'foo'); 0",
        "StrComp('FOO', 'bar', 0); -1",
        "StrComp('bar', 'FOO', 0); 1",
        "StrComp('FOO', 'foo', 0); -1"
    })
    void testFuncsInt(String exprStr, int expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = ';', value = {
        "Str(Null)",
        "InStr(3, Null, 'FOO')",
        "InStrRev(Null, 'FOO', 3)",
        "Choose(-1,'foo','bar','blah')",
        "Switch(False,'foo', False, 'bar', False, 'blah')"
    })
    void testFuncsNull(String exprStr) {
        assertNull(eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "CDbl(\"57.12345\"); 57.12345",
        "Val('    1615 198th Street N.E.'); 1615198d",
        "Val('  &HFFFFwhatever'); -1d",
        "Val('  &H1FFFFwhatever'); 131071d",
        "Val('  &HFFFFFFFFwhatever'); -1d",
        "Val('  &H123whatever'); 291d",
        "Val('  &O123whatever'); 83d",
        "Val('  1 2 3 e -2 whatever'); 1.23d",
        "Val('  whatever123 '); 0d",
        "Val(''); 0d"
    })
    void testFuncsDouble(String exprStr, double expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "CCur(\"57.12346\"); 57.1235",
        "CDec(\"57.123456789\"); 57.123456789"
    })
    void testFuncsBigDecimal(String exprStr, BigDecimal expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "CSng(\"57.12345\"); 57.12345"
    })
    void testFuncsFloat(String exprStr, String expected) {
        assertEquals(Float.valueOf(expected).doubleValue(), eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "2003, 1, 2, 0, 0; CDate('01/02/2003')",
        "2003, 1, 2, 7, 0; CDate('01/02/2003 7:00:00 AM')",
        "1908, 3, 31, 10, 48; CDate(3013.45)"
    })
    void testFuncsLocalDateTime(@ConvertWith(CsvToLocalDateTime.class) LocalDateTime expected, String exprStr) {
        assertEquals(expected, eval(exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "Str$(Null); Value[NULL] 'null' cannot be converted to STRING",
        "StrReverse('blah', 1); Invalid function call",
        "StrReverse(); Invalid function call"
    })
    void testFuncsExceptions(String exprStr, String message) {
        EvalException ex = assertThrows(EvalException.class, () -> eval('=' + exprStr));
        assertTrue(ex.getMessage().contains(message));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', quoteCharacter = '\"', value = {
        "Format(12345.6789, 'General Number'); 12345.6789",
        "Format(0.12345, 'General Number'); 0.12345",
        "Format(-12345.6789, 'General Number'); -12345.6789",
        "Format(-0.12345, 'General Number'); -0.12345",
        "Format('12345.6789', 'General Number'); 12345.6789",
        "Format('1.6789E+3', 'General Number'); 1678.9",
        "Format(#01/02/2003 7:00:00 AM#, 'General Number'); 37623.2916666667",
        "Format('foo', 'General Number'); foo",

        "Format(12345.6789, 'Standard'); 12,345.68",
        "Format(0.12345, 'Standard'); 0.12",
        "Format(-12345.6789, 'Standard'); -12,345.68",
        "Format(-0.12345, 'Standard'); -0.12",

        "Format(12345.6789, 'Fixed'); 12345.68",
        "Format(0.12345, 'Fixed'); 0.12",
        "Format(-12345.6789, 'Fixed'); -12345.68",
        "Format(-0.12345, 'Fixed'); -0.12",

        "Format(12345.6789, 'Euro'); €12,345.68",
        "Format(0.12345, 'Euro'); €0.12",
        "Format(-12345.6789, 'Euro'); (€12,345.68)",
        "Format(-0.12345, 'Euro'); (€0.12)",

        "Format(12345.6789, 'Currency'); $12,345.68",
        "Format(0.12345, 'Currency'); $0.12",
        "Format(-12345.6789, 'Currency'); ($12,345.68)",
        "Format(-0.12345, 'Currency'); ($0.12)",

        "Format(12345.6789, 'Percent'); 1234567.89%",
        "Format(0.12345, 'Percent'); 12.34%",
        "Format(-12345.6789, 'Percent'); -1234567.89%",
        "Format(-0.12345, 'Percent'); -12.34%",

        "Format(12345.6789, 'Scientific'); 1.23E+4",
        "Format(0.12345, 'Scientific'); 1.23E-1",
        "Format(-12345.6789, 'Scientific'); -1.23E+4",
        "Format(-0.12345, 'Scientific'); -1.23E-1",

        "Format(True, 'Yes/No'); Yes",
        "Format(False, 'Yes/No'); No",
        "Format(True, 'True/False'); True",
        "Format(False, 'True/False'); False",
        "Format(True, 'On/Off'); On",
        "Format(False, 'On/Off'); Off",

        "Format(#01/02/2003 7:00:00 AM#, 'General Date'); 1/2/2003 7:00:00 AM",
        "Format(#01/02/2003#, 'General Date'); 1/2/2003",
        "Format(#7:00:00 AM#, 'General Date'); 7:00:00 AM",
        "Format('37623.2916666667', 'General Date'); 1/2/2003 7:00:00 AM",
        "Format('foo', 'General Date'); foo",
        "Format('', 'General Date'); \"\"",

        "Format(#01/02/2003 7:00:00 AM#, 'Long Date'); Thursday, January 02, 2003",
        "Format(#01/02/2003 7:00:00 AM#, 'Medium Date'); 02-Jan-03",
        "Format(#01/02/2003 7:00:00 AM#, 'Short Date'); 1/2/2003",
        "Format(#01/02/2003 7:00:00 AM#, 'Long Time'); 7:00:00 AM",
        "Format(#01/02/2003 7:00:00 AM#, 'Medium Time'); 07:00 AM",
        "Format(#01/02/2003 7:00:00 AM#, 'Short Time'); 07:00",
        "Format(#01/02/2003 7:00:00 PM#, 'Short Time'); 19:00"
    })
    void testFormat(String exprStr, String expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '§', value = {
        "Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p')| 07:00 a",
        "Format(#01/10/2003 7:00:00 PM#, 'hh:nn a/p')| 07:00 p",
        "Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p w ww')| 07:00 a 6 2",
        "Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p w ww', 3, 3)| 07:00 a 4 1",
        "Format(#01/10/2003 7:13:00 AM#, 'nnnn; foo bar')| 1313",
        "Format(#01/10/2003 7:13:00 AM#, 'q c ttt \"this is text\"')| 1 1/10/2003 7:13:00 AM ttt this is text",
        "Format(#01/10/2003#, 'q c ttt \"this is text\"')| 1 1/10/2003 ttt this is text",
        "Format(#7:13:00 AM#, \"q c ttt \"\"this 'is' \"\"\"\"text\"\"\"\"\"\"\")| 4 7:13:00 AM ttt this 'is' \"text\"",
        "Format('true', 'c')| 12/29/1899",
        "Format('3.9', '*~dddd, yy mmm d, hh:nn:ss \\Y[Yellow]')| Tuesday, 00 Jan 2, 21:36:00 Y",
        "Format('3.9', 'dddd, yy mmm mm/d, hh:nn:ss AMPM')| Tuesday, 00 Jan 01/2, 09:36:00 PM",
        "Format('3.9', 'ttttt')| 9:36:00 PM",
        "Format(3.9, 'ttttt')| 9:36:00 PM",
        "Format('foo', 'dddd, yy mmm mm d, hh:nn:ss AMPM')| foo"
    })
    void testCustomFormat1(String exprStr, String expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "';\\y;\\n'", testValues = {
        "foo", "'foo'",
        "", "''",
        "y", "True",
        "n", "'0'",
        "", "Null"})
    void testCustomFormat2(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\\p;\"y\";!\\n;*~\\z[Blue];'", testValues = {
        "foo", "'foo'",
        "", "''",
        "y", "True",
        "n", "'0'",
        "p", "'10'",
        "z", "Null"
    })
    void testCustomFormat3(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\"p\"#.00#\"blah\"'", testValues = {
        "p13.00blah", "13",
        "-p13.00blah", "-13",
        "p.00blah", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat4(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\"p\"#.00#\"blah\";(\"p\"#.00#\"blah\")'", testValues = {
        "p13.00blah", "13",
        "(p13.00blah)", "-13",
        "p.00blah", "0",
        "(p1.00blah)", "True",
        "p.00blah", "'false'",
        "p37623.292blah", "#01/02/2003 7:00:00 AM#",
        "p37623.292blah", "'01/02/2003 7:00:00 AM'",
        "NotANumber", "'NotANumber'",
        "", "''",
        "", "Null"
    })
    void testCustomFormat5(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\"p\"#.00#\"blah\";!(\"p\"#.00#\"blah\")[Red];\"zero\"'", testValues = {
        "p13.00blah", "13",
        "(p13.00blah)", "-13",
        "zero", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat6(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\\p#.00#\"blah\";*~(\"p\"#.00#\"blah\");\"zero\";\"yuck\"'", testValues = {
        "p13.00blah", "13",
        "(p13.00blah)", "-13",
        "zero", "0",
        "", "''",
        "yuck", "Null"
    })
    void testCustomFormat7(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.##;(0.###);\"zero\";\"yuck\";'", testValues = {
        "0.03", "0.03",
        "zero", "0.003",
        "(0.003)", "-0.003",
        "zero", "-0.0003"
    })
    void testCustomFormat8(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.##;(0.###E+0)'", testValues = {
        "0.03", "0.03",
        "(3.E-4)", "-0.0003",
        "0.", "0",
        "34223.", "34223",
        "(3.422E+4)", "-34223"
    })
    void testCustomFormat9(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.###E-0'", testValues = {
        "3.E-4", "0.0003",
        "3.422E4", "34223"
    })
    void testCustomFormat10(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.###e+0'", testValues = {
        "3.e-4", "0.0003",
        "3.422e+4", "34223"
    })
    void testCustomFormat11(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.###e-0'", testValues = {
        "3.e-4", "0.0003",
        "3.422e4", "34223"
    })
    void testCustomFormat12(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'#,##0.###'", testValues = {
        "0.003", "0.003",
        "0.", "0.0003",
        "34,223.", "34223"
    })
    void testCustomFormat13(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.'", testValues = {
        "13.", "13",
        "0.", "0.003",
        "-45.", "-45",
        "0.", "-0.003",
        "0.", "0"
    })
    void testCustomFormat14(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0.#'", testValues = {
        "13.", "13",
        "0.3", "0.3",
        "0.", "0.003",
        "-45.", "-45",
        "0.", "-0.003",
        "0.", "0"
    })
    void testCustomFormat15(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'0'", testValues = {
        "13", "13",
        "0", "0.003",
        "-45", "-45",
        "0", "-0.003",
        "0", "0"
    })
    void testCustomFormat16(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'%0'", testValues = {
        "%13", "0.13",
        "%0", "0.003",
        "-%45", "-0.45",
        "%0", "-0.003",
        "%0", "0"
    })
    void testCustomFormat17(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'#'", testValues = {
        "13", "13",
        "", "0.003",
        "-45", "-45",
        "", "-0.003",
        "", "0"
    })
    void testCustomFormat18(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\\0\\[#.#\\]\\0'", testValues = {
        "0[13.]0", "13",
        "0[.]0", "0.003",
        "0[.3]0", "0.3",
        "-0[45.]0", "-45",
        "0[.]0", "-0.003",
        "-0[.3]0", "-0.3",
        "0[.]0", "0"
    })
    void testCustomFormat19(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "\"#;n'g;'\"", testValues = {
        "5", "5",
        "n'g", "-5",
        "'", "0"
    })
    void testCustomFormat20(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'$0.0#'", testValues = {
        "$213.0", "213"
    })
    void testCustomFormat21(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'@'", testValues = {
        "foo", "'foo'",
        "-13", "-13",
        "0", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat22(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'>@'", testValues = {
        "FOO", "'foo'",
        "-13", "-13",
        "0", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat23(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'<@'", testValues = {
        "foo", "'FOO'",
        "-13", "-13",
        "0", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat24(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'!>@;'", testValues = {
        "O", "'foo'",
        "3", "-13",
        "0", "0",
        "", "''",
        "", "Null"
    })
    void testCustomFormat25(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'!>*~@[Red];\"empty\";'", testValues = {
        "O", "'foo'",
        "3", "-13",
        "0", "0",
        "empty", "''",
        "empty", "Null"
    })
    void testCustomFormat26(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'><@'", testValues = {
        "fOo", "'fOo'"
    })
    void testCustomFormat27(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\\x@@@&&&\\y'", testValues = {
        "x   fy", "'f'",
        "x   fooy", "'foo'",
        "x foobay", "'fooba'",
        "xfoobarybaz", "'foobarbaz'"
    })
    void testCustomFormat28(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'!\\x@@@&&&\\y'", testValues = {
        "xf  y", "'f'",
        "xfooy", "'foo'",
        "xfoobay", "'fooba'",
        "xbarbazy", "'foobarbaz'"
    })
    void testCustomFormat29(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'\\x&&&@@@\\y'", testValues = {
        "x  fy", "'f'",
        "xfooy", "'foo'",
        "xfoobay", "'fooba'",
        "xfoobarybaz", "'foobarbaz'"
    })
    void testCustomFormat30(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] Format({0}, {1}) --> '{2}'")
    @CustomFormatSource(format = "'!\\x&&&@@@\\y'", testValues = {
        "xf   y", "'f'",
        "xfoo   y", "'foo'",
        "xfooba y", "'fooba'",
        "xbarbazy", "'foobarbaz'"
    })
    void testCustomFormat31(String value, String fmtStr, String expected) {
        assertEquals(expected, eval("=Format(" + value + ", " + fmtStr + ")"));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "Abs(1)   ; 1",
        "Abs(-1)  ; 1",
        "Abs(-1.1); 1.1"
    })
    void testNumberFuncsInt(String exprStr, double expected) {
        Number result = (Number) eval('=' + exprStr);
        assertEquals(expected, result.doubleValue());
    }

    @Test
    void testNumberFuncsMath() {
        assertAll("trig",
            () -> assertEquals(Math.atan(0.2), eval("=Atan(0.2)")),
            () -> assertEquals(Math.sin(0.2), eval("=Sin(0.2)")),
            () -> assertEquals(Math.tan(0.2), eval("=Tan(0.2)")),
            () -> assertEquals(Math.cos(0.2), eval("=Cos(0.2)")),
            () -> assertEquals(Math.exp(0.2), eval("=Exp(0.2)")),
            () -> assertEquals(Math.log(0.2), eval("=Log(0.2)")),
            () -> assertEquals(Math.sqrt(4.3), eval("=Sqr(4.3)"))
        );
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "Fix(3.5) ; 3",
        "Fix(4)   ; 4",
        "Fix(-3.5); -3",
        "Fix(-4)  ; -4",

        "Sgn(3.5) ; 1",
        "Sgn(4)   ; 1",
        "Sgn(-3.5); -1",
        "Sgn(-4)  ; -1",

        "Int(3.5) ; 3",
        "Int(4)   ; 4",
        "Int(-3.5); -4",
        "Int(-4)  ; -4 "
    })
    void testNumberFuncsInt2(String exprStr, int expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "Round(3.7) ;  4; true",
        "Round(4)   ;  4; false",
        "Round(-3.7); -4; true",
        "Round(-4)  ; -4; false",

        "Round(3.7345, 2) ;  3.73; true",
        "Round(4, 2)      ;     4; false",
        "Round(-3.7345, 2); -3.73; true",
        "Round(-4, 2)     ;    -4; false"
    })
    void testNumberFuncsRound(String exprStr, Double expected, boolean bd) {
        assertEquals(bd ? toBD(expected) : expected.intValue(), eval('=' + exprStr));
    }

    static Stream<Arguments> getDateFuncsTestData() {
        return Stream.of(
            arguments("CStr(DateValue(#01/02/2003 7:00:00 AM#))", "1/2/2003"),
            arguments("CStr(TimeValue(#01/02/2003 7:00:00 AM#))", "7:00:00 AM"),

            arguments("CStr(#13:10:00#)", "1:10:00 PM"),

            arguments("Year(#01/02/2003 7:00:00 AM#)", 2003),
            arguments("Month(#01/02/2003 7:00:00 AM#)", 1),
            arguments("Day(#01/02/2003 7:00:00 AM#)", 2),

            arguments("Year('01/02/2003 7:00:00 AM')", 2003),
            arguments("Year(#7:00:00 AM#)", 1899),
            arguments("Year('01/02 7:00:00 AM')", Calendar.getInstance().get(Calendar.YEAR)),

            arguments("MonthName(1)", "January"),
            arguments("MonthName(2,True)", "Feb"),
            arguments("MonthName(3,False)", "March"),

            arguments("Hour(#01/02/2003 7:10:27 AM#)", 7),
            arguments("Hour(#01/02/2003 7:10:27 PM#)", 19),
            arguments("Minute(#01/02/2003 7:10:27 AM#)", 10),
            arguments("Second(#01/02/2003 7:10:27 AM#)", 27),

            arguments("Weekday(#11/22/2003#)", 7),
            arguments("Weekday(#11/22/2003#, 5)", 3),
            arguments("Weekday(#11/22/2003#, 7)", 1),

            arguments("WeekdayName(1)", "Sunday"),
            arguments("WeekdayName(1,True)", "Sun"),
            arguments("WeekdayName(1,False,3)", "Tuesday"),
            arguments("WeekdayName(3,True,3)", "Thu"),

            arguments("CStr(Date())", (Supplier<?>) () ->
                DateTimeFormatter.ofPattern("M/d/yyyy").format(LocalDate.now(TestUtil.TEST_TZ.toZoneId()))),
            arguments("CStr(Time())", (Supplier<?>) () ->
                new DateTimeFormatterBuilder().appendPattern("h:mm:ss a").toFormatter(Locale.US).format(LocalDateTime.now(TestUtil.TEST_TZ.toZoneId()))),

            arguments("CStr(TimeSerial(3,57,34))", "3:57:34 AM"),
            arguments("CStr(TimeSerial(15,57,34))", "3:57:34 PM"),
            arguments("CStr(TimeSerial(6,-15,0))", "5:45:00 AM"),
            arguments("CStr(TimeSerial(0,0,0))", "12:00:00 AM"),
            arguments("CStr(TimeSerial(-10,0,0))", "2:00:00 PM"),
            arguments("CStr(TimeSerial(30,0,0))", "6:00:00 AM"),

            arguments("CStr(DateSerial(69,2,12))", "2/12/1969"),
            arguments("CStr(DateSerial(10,2,12))", "2/12/2010"),
            arguments("CStr(DateSerial(2014,-5,12))", "7/12/2013"),
            arguments("CStr(DateSerial(2014,-5,38))", "8/7/2013"),

            arguments("DatePart('ww',#01/03/2018#)", 1),
            arguments("DatePart('ww',#01/03/2018#,4)", 2),
            arguments("DatePart('ww',#01/03/2018#,5)", 1),
            arguments("DatePart('ww',#01/03/2018#,4,3)", 1),
            arguments("DatePart('ww',#01/03/2018#,5,3)", 52),
            arguments("DatePart('ww',#01/03/2018#,4,2)", 1),
            arguments("DatePart('ww',#01/03/2018#,5,2)", 53),
            arguments("DatePart('yyyy',#11/22/2003 5:45:13 AM#)", 2003),
            arguments("DatePart('q',#11/22/2003 5:45:13 AM#)", 4),
            arguments("DatePart('m',#11/22/2003 5:45:13 AM#)", 11),
            arguments("DatePart('y',#11/22/2003 5:45:13 AM#)", 326),
            arguments("DatePart('d',#11/22/2003 5:45:13 AM#)", 22),
            arguments("DatePart('w',#11/22/2003 5:45:13 AM#)", 7),
            arguments("DatePart('w',#11/22/2003 5:45:13 AM#, 5)", 3),
            arguments("DatePart('h',#11/22/2003 5:45:13 AM#)", 5),
            arguments("DatePart('n',#11/22/2003 5:45:13 AM#)", 45),
            arguments("DatePart('s',#11/22/2003 5:45:13 AM#)", 13),

            arguments("CStr(DateAdd('yyyy',2,#11/22/2003 5:45:13 AM#))", "11/22/2005 5:45:13 AM"),
            arguments("CStr(DateAdd('q',1,#11/22/2003 5:45:13 AM#))", "2/22/2004 5:45:13 AM"),
            arguments("CStr(DateAdd('m',2,#11/22/2003 5:45:13 AM#))", "1/22/2004 5:45:13 AM"),
            arguments("CStr(DateAdd('d',20,#11/22/2003 5:45:13 AM#))", "12/12/2003 5:45:13 AM"),
            arguments("CStr(DateAdd('w',20,#11/22/2003 5:45:13 AM#))", "12/12/2003 5:45:13 AM"),
            arguments("CStr(DateAdd('y',20,#11/22/2003 5:45:13 AM#))", "12/12/2003 5:45:13 AM"),
            arguments("CStr(DateAdd('ww',5,#11/22/2003 5:45:13 AM#))", "12/27/2003 5:45:13 AM"),
            arguments("CStr(DateAdd('h',10,#11/22/2003 5:45:13 AM#))", "11/22/2003 3:45:13 PM"),
            arguments("CStr(DateAdd('n',34,#11/22/2003 5:45:13 AM#))", "11/22/2003 6:19:13 AM"),
            arguments("CStr(DateAdd('s',74,#11/22/2003 5:45:13 AM#))", "11/22/2003 5:46:27 AM"),

            arguments("CStr(DateAdd('d',20,#11/22/2003#))", "12/12/2003"),
            arguments("CStr(DateAdd('h',10,#11/22/2003#))", "11/22/2003 10:00:00 AM"),
            arguments("CStr(DateAdd('h',24,#11/22/2003#))", "11/23/2003"),
            arguments("CStr(DateAdd('h',10,#5:45:13 AM#))", "3:45:13 PM"),
            arguments("CStr(DateAdd('h',30,#5:45:13 AM#))", "12/31/1899 11:45:13 AM"),

            arguments("DateDiff('yyyy',#10/22/2003#,#11/22/2003#)", 0),
            arguments("DateDiff('yyyy',#10/22/2003#,#11/22/2007#)", 4),
            arguments("DateDiff('yyyy',#11/22/2007#,#10/22/2003#)", -4),

            arguments("DateDiff('q',#10/22/2003#,#11/22/2003#)", 0),
            arguments("DateDiff('q',#03/01/2003#,#11/22/2003#)", 3),
            arguments("DateDiff('q',#10/22/2003#,#11/22/2007#)", 16),
            arguments("DateDiff('q',#03/22/2007#,#10/22/2003#)", -13),

            arguments("DateDiff('m',#10/22/2003#,#11/01/2003#)", 1),
            arguments("DateDiff('m',#03/22/2003#,#11/01/2003#)", 8),
            arguments("DateDiff('m',#10/22/2003#,#11/22/2007#)", 49),
            arguments("DateDiff('m',#03/22/2007#,#10/01/2003#)", -41),

            arguments("DateDiff('d','10/22','11/01')", 10),
            arguments("DateDiff('y',#1:37:00 AM#,#2:15:00 AM#)", 0),
            arguments("DateDiff('d',#10/22/2003#,#11/01/2003#)", 10),
            arguments("DateDiff('d',#10/22/2003 11:00:00 PM#,#10/23/2003 1:00:00 AM#)", 1),
            arguments("DateDiff('d',#03/22/2003#,#11/01/2003#)", 224),
            arguments("DateDiff('y',#10/22/2003#,#11/22/2007#)", 1492),
            arguments("DateDiff('d',#03/22/2007#,#10/01/2003#)", -1268),
            arguments("DateDiff('d',#1/1/2000#,#1/1/2001#)", 366),
            arguments("DateDiff('d',#1/1/2001#,#1/1/2002#)", 365),

            arguments("DateDiff('w',#11/3/2018#,#11/04/2018#)", 0),
            arguments("DateDiff('w',#11/3/2018#,#11/10/2018#)", 1),
            arguments("DateDiff('w',#12/31/2017#,#1/1/2018#)", 0),
            arguments("DateDiff('w',#03/22/2003#,#11/01/2003#)", 32),
            arguments("DateDiff('w',#10/22/2003#,#11/22/2007#)", 213),
            arguments("DateDiff('w',#03/22/2007#,#10/01/2003#)", -181),

            arguments("DateDiff('ww',#11/3/2018#,#11/04/2018#)", 1),
            arguments("DateDiff('ww',#11/3/2018#,#11/10/2018#)", 1),
            arguments("DateDiff('ww',#12/31/2017#,#1/1/2018#)", 0),
            arguments("DateDiff('ww',#12/31/2017#,#1/1/2018#,2)", 1),
            arguments("DateDiff('ww',#12/31/2017#,#1/1/2018#,1,3)", 0),
            arguments("DateDiff('ww',#1/1/2000#,#1/1/2001#)", 53),
            arguments("DateDiff('ww',#03/22/2003#,#11/01/2003#)", 32),
            arguments("DateDiff('ww',#10/22/2003#,#11/22/2007#)", 213),
            arguments("DateDiff('ww',#03/22/2007#,#10/01/2003#)", -181),

            arguments("DateDiff('h',#1:37:00 AM#,#2:15:00 AM#)", 1),
            arguments("DateDiff('h',#1:37:00 AM#,#2:15:00 PM#)", 13),
            arguments("DateDiff('h',#11/3/2018 1:37:00 AM#,#11/3/2018 2:15:00 AM#)", 1),
            arguments("DateDiff('h',#11/3/2018 1:37:00 AM#,#11/3/2018 2:15:00 PM#)", 13),
            arguments("DateDiff('h',#11/3/2018#,#11/4/2018#)", 24),
            arguments("DateDiff('h',#3/13/2018 1:37:00 AM#,#11/3/2018 2:15:00 AM#)", 5641),
            arguments("DateDiff('h',#3/13/2016 1:37:00 AM#,#11/3/2018 2:15:00 AM#)", 23161),
            arguments("DateDiff('h',#11/3/2018 2:15:00 PM#,#3/13/2016 1:37:00 AM#)", -23173),

            arguments("DateDiff('n',#1:37:59 AM#,#1:38:00 AM#)", 1),
            arguments("DateDiff('n',#1:37:30 AM#,#2:15:13 PM#)", 758),
            arguments("DateDiff('n',#11/3/2018 1:37:59 AM#,#11/3/2018 1:38:00 AM#)", 1),
            arguments("DateDiff('n',#11/3/2018 1:37:59 AM#,#11/3/2018 2:15:00 PM#)", 758),
            arguments("DateDiff('n',#11/3/2018#,#11/4/2018#)", 1440),
            arguments("DateDiff('n',#3/13/2018 1:37:59 AM#,#11/3/2018 2:15:00 AM#)", 338438),
            arguments("DateDiff('n',#3/13/2016 1:37:30 AM#,#11/3/2018 2:15:13 AM#)", 1389638),
            arguments("DateDiff('n',#11/3/2018 2:15:30 PM#,#3/13/2016 1:37:13 AM#)", -1390358),

            arguments("DateDiff('s',#1:37:59 AM#,#1:38:00 AM#)", 1),
            arguments("DateDiff('s',#1:37:10 AM#,#1:37:45 AM#)", 35),
            arguments("DateDiff('s',#1:37:30 AM#,#2:15:13 PM#)", 45463),
            arguments("DateDiff('s',#11/3/2018 1:37:59 AM#,#11/3/2018 1:38:00 AM#)", 1),
            arguments("DateDiff('s',#11/3/2018 1:37:30 AM#,#11/3/2018 2:15:13 PM#)", 45463),
            arguments("DateDiff('s',#11/3/2018#,#11/4/2018#)", 86400),
            arguments("DateDiff('s',#3/13/2018 1:37:59 AM#,#11/3/2018 2:15:00 AM#)", 20306221),
            arguments("DateDiff('s',#3/13/2016 1:37:30 AM#,#11/3/2018 2:15:13 AM#)", 83378263),
            arguments("DateDiff('s',#11/3/2018 2:15:30 PM#,#3/13/2016 1:37:13 AM#)", -83421497)
            );
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @MethodSource("getDateFuncsTestData")
    void testDateFuncs(String exprStr, Object expected) {
        if (expected instanceof Supplier) {
            expected = ((Supplier<Object>) expected).get();
        }
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {0} --> {1}")
    @CsvSource(delimiter = ';', value = {
        "CStr(NPer(0.12/12,-100,-1000)); -9.57859403981306",
        "CStr(NPer(0.12/12,-100,-1000,0,1)); -9.48809500550578",
        "CStr(NPer(0.12/12,-100,-1000,10000)); 60.0821228537616",
        "CStr(NPer(0.12/12,-100,-1000,10000,1)); 59.6738656742947",
        "CStr(NPer(0.12/12,-100,0,10000)); 69.6607168935747",
        "CStr(NPer(0.12/12,-100,0,10000,1)); 69.1619606798005",

        "CStr(FV(0.12/12,60,-100)); 8166.96698564091",
        "CStr(FV(0.12/12,60,-100,0,1)); 8248.63665549732",
        "CStr(FV(0.12/12,60,-100,1000)); 6350.27028707682",
        "CStr(FV(0.12/12,60,-100,1000,1)); 6431.93995693323",

        "CStr(PV(0.12/12,60,-100)); 4495.5038406224",
        "CStr(PV(0.12/12,60,-100,0,1)); 4540.45887902863",
        "CStr(PV(0.12/12,60,-100,10000)); -1008.99231875519",
        "CStr(PV(0.12/12,60,-100,10000,1)); -964.03728034897",

        "CStr(Pmt(0.12/12,60,-1000)); 22.2444476849018",
        "CStr(Pmt(0.12/12,60,-1000,0,1)); 22.0242056286156",
        "CStr(Pmt(0.12/12,60,-1000,10000)); -100.200029164116",
        "CStr(Pmt(0.12/12,60,-1000,10000,1)); -99.2079496674414",
        "CStr(Pmt(0.12/12,60,0,10000)); -122.444476849018",
        "CStr(Pmt(0.12/12,60,0,10000,1)); -121.232155296057",
        "CStr(Pmt(0.12/12,60,-1000)); 22.2444476849018",

        "CStr(IPmt(0.12/12,1,60,-1000)); 10",
        "CStr(IPmt(0.12/12,30,60,-1000)); 5.90418478297567",
        "CStr(IPmt(0.12/12,1,60,-1000,0,1)); 0",
        "CStr(IPmt(0.12/12,30,60,-1000,0,1)); 5.8457275078967",
        "CStr(IPmt(0.12/12,1,60,0,10000)); 0",
        "CStr(IPmt(0.12/12,30,60,0,10000)); 40.9581521702433",
        "CStr(IPmt(0.12/12,1,60,0,10000,1)); 0",
        "CStr(IPmt(0.12/12,30,60,0,10000,1)); 40.552625911132",
        "CStr(IPmt(0.12/12,1,60,-1000,10000)); 10",
        "CStr(IPmt(0.12/12,30,60,-1000,10000)); 46.862336953219",
        "CStr(IPmt(0.12/12,1,60,-1000,10000,1)); 0",
        "CStr(IPmt(0.12/12,30,60,-1000,10000,1)); 46.3983534190287",

        "CStr(PPmt(0.12/12,1,60,-1000)); 12.2444476849018",
        "CStr(PPmt(0.12/12,30,60,-1000)); 16.3402629019261",
        "CStr(PPmt(0.12/12,1,60,-1000,0,1)); 22.0242056286156",
        "CStr(PPmt(0.12/12,30,60,-1000,0,1)); 16.1784781207189",
        "CStr(PPmt(0.12/12,1,60,0,10000)); -122.444476849018",
        "CStr(PPmt(0.12/12,30,60,0,10000)); -163.402629019261",
        "CStr(PPmt(0.12/12,1,60,0,10000,1)); -121.232155296057",
        "CStr(PPmt(0.12/12,30,60,0,10000,1)); -161.784781207189",
        "CStr(PPmt(0.12/12,1,60,-1000,10000)); -110.200029164116",
        "CStr(PPmt(0.12/12,30,60,-1000,10000)); -147.062366117335",
        "CStr(PPmt(0.12/12,1,60,-1000,10000,1)); -99.2079496674414",
        "CStr(PPmt(0.12/12,30,60,-1000,10000,1)); -145.60630308647",

        "CStr(DDB(2400,300,10*365,1)); 1.31506849315068",
        "CStr(DDB(2400,300,10*12,1)); 40",
        "CStr(DDB(2400,300,10,1)); 480",
        "CStr(DDB(2400,300,10,10)); 22.1225472000002",
        "CStr(DDB(2400,300,10,4)); 245.76",
        "CStr(DDB(2400,300,10,3)); 307.2",
        "CStr(DDB(2400,300,10,0.1)); 480",
        "CStr(DDB(2400,300,10,3.5)); 274.768033075174",

        "CStr(SLN(30000,7500,10)); 2250",
        "CStr(SLN(10000,5000,5)); 1000",
        "CStr(SLN(8000,0,7)); 1142.85714285714",

        "CStr(SYD(30000,7500,10,1)); 4090.90909090909",
        "CStr(SYD(30000,7500,10,10)); 409.090909090909",

        "CStr(Rate(3,200,-610,0,-20,0.1)); -1.63048347266756E-02",
        // the result of this varies slightly depending on the jvm impl, so we
        // round it to fewer decimal places (7.70147248820165E-03 or
        // 7.70147248820155E-03)
        "Format(CStr(Rate(4*12,-200,8000)), '#.############E+00'); 7.701472488202E-03",
        "CStr(Rate(60,93.22,5000,0.1)); -1.09802980531205"
    })
    void testFinancialFuncs(String exprStr, String expected) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(CustomFormatArgumentsProvider.class)
    static @interface CustomFormatSource {
        String format();

        String[] testValues() default {};
    }

    static class CustomFormatArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<CustomFormatSource> {
        private CustomFormatSource config;

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            // context.getElement().ifPresent(annotatedElement -> System.out.println("Element (CustomFormatSource.class): " + AnnotationSupport.findRepeatableAnnotations(annotatedElement, CustomFormatSource.class)));
            String[] testValues = config.testValues();
            return IntStream.range(0, testValues.length).filter(i -> i % 2 == 0).mapToObj(i -> {
                String expected = testValues[i];
                String val = testValues[i + 1];
                return Arguments.of(val, config.format(), expected);
            });
        }

        @Override
        public void accept(CustomFormatSource source) {
            config = source;
        }
    }

}
