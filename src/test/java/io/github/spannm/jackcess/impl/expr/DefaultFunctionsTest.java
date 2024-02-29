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

import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.converter.CsvToLocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;

/**
 * @author James Ahlborn
 */
class DefaultFunctionsTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "foo; IIf(10 > 1, \"foo\", \"bar\")",
            "bar; IIf(10 < 1, \"foo\", \"bar\")",
            "f; Chr(102)",
            "☺; ChrW(9786)",
            "263A; Hex(9786)",
            "blah; Nz(\"blah\")",
            "\"\"; Nz(Null)",
            "blah; Nz(\"blah\",\"FOO\")",
            "FOO; Nz(Null,\"FOO\")",
            "23072; Oct(9786)",
            "\" 9786\"; Str(9786)",
            "-42; Str(-42)",
            "-42; Str$(-42)",

            "9786; CStr(9786)",
            "-42; CStr(-42)",
            "Null; TypeName(Null)",
            "String; TypeName('blah')",
            "Date; TypeName(#01/02/2003#)",
            "Long; TypeName(42)",
            "Double; TypeName(CDbl(42))",
            "Decimal; TypeName(42.3)",

            "FOOO; UCase(\"fOoO\")",
            "fooo; LCase(\"fOoO\")",

            "bl; Left(\"blah\", 2)",
            "\"\"; Left(\"blah\", 0)",
            "blah; Left(\"blah\", 17)",
            "la; Mid(\"blah\", 2, 2)",

            "ah; Right(\"blah\", 2)",
            "\"\"; Right(\"blah\", 0)",
            "blah; Right(\"blah\", 17)",

            "\"blah  \"; LTrim(\"  blah  \")",
            "\"  blah\"; RTrim(\"  blah  \")",
            "blah; Trim(\"  blah  \")",
            "\"   \"; Space(3)",
            "ddd; String(3,'d')",

            "FOO; StrConv('foo', 1)",
            "foo; StrConv('foo', 2)",
            "foo; StrConv('FOO', 2)",
            "Foo Bar; StrConv('FOO bar', 3)",

            "halb; StrReverse('blah')",

            "foo; Choose(1,'foo','bar','blah')",

            "blah; Choose(3,'foo','bar','blah')",
            "bar; Switch(False,'foo', True, 'bar', True, 'blah')",
            "blah; Switch(False,'foo', False, 'bar', True, 'blah')",
            "faa; Replace('foo','o','a')",
            "faa; Replace('fOo','o','a')",
            "aa; Replace('foo','o','a',2)",
            "oo; Replace('foo','o','a',2,0)",
            "\"\"; Replace('foo','o','a',4)",
            "foo; Replace('foo','','a')",
            "o; Replace('foo','','a',3)",
            "fahhabahhaahha; Replace('fooboooo','OO','ahha')",
            "fahhaboooo; Replace('fooboooo','OO','ahha',1,1)",
            "fooboooo; Replace('fooboooo','OO','ahha',1,1,0)",
            "ahhabahhaahha; Replace('fooboooo','OO','ahha',2)",
            "obahhaahha; Replace('fooboooo','OO','ahha',3)",
            "fb; Replace('fooboooo','OO','')",
            "\"\"; Replace('','o','a')",
            "foo; Replace('foo','foobar','a')",
            "12,345.00; FormatNumber(12345)",
            "0.12; FormatNumber(0.12345)",
            "12.34; FormatNumber(12.345)",
            "-12,345.00; FormatNumber(-12345)",
            "-0.12; FormatNumber(-0.12345)",
            "-12.34; FormatNumber(-12.345)",
            "12,345.000; FormatNumber(12345,3)",
            "0.123; FormatNumber(0.12345,3)",
            "12.345; FormatNumber(12.345,3)",
            "12,345; FormatNumber(12345,0)",
            "0; FormatNumber(0.12345,0)",
            "12; FormatNumber(12.345,0)",
            "0.123; FormatNumber(0.12345,3,True)",
            ".123; FormatNumber(0.12345,3,False)",
            "-0.123; FormatNumber(-0.12345,3,True)",
            "-.123; FormatNumber(-0.12345,3,False)",
            "-12.34; FormatNumber(-12.345,-1,True,False)",
            "(12.34); FormatNumber(-12.345,-1,True,True)",
            "(12); FormatNumber(-12.345,0,True,True)",
            "12,345.00; FormatNumber(12345,-1,-2,-2,True)",
            "12345.00; FormatNumber(12345,-1,-2,-2,False)",

            "1,234,500.00%; FormatPercent(12345)",
            "(1,234.50%); FormatPercent(-12.345,-1,True,True)",
            "34%; FormatPercent(0.345,0,True,True)",
            "-.123%; FormatPercent(-0.0012345,3,False)",

            "$12,345.00; FormatCurrency(12345)",
            "($12,345.00); FormatCurrency(-12345)",
            "-$12.34; FormatCurrency(-12.345,-1,True,False)",
            "$12; FormatCurrency(12.345,0,True,True)",
            "($.123); FormatCurrency(-0.12345,3,False)",

            "1/1/1973 1:37:25 PM; FormatDateTime(#1/1/1973 1:37:25 PM#)",
            "1:37:25 PM; FormatDateTime(#1:37:25 PM#,0)",
            "1/1/1973; FormatDateTime(#1/1/1973#,0)",
            "Monday, January 01, 1973; FormatDateTime(#1/1/1973 1:37:25 PM#,1)",
            "1/1/1973; FormatDateTime(#1/1/1973 1:37:25 PM#,2)",
            "1:37:25 PM; FormatDateTime(#1/1/1973 1:37:25 PM#,3)",
            "13:37; FormatDateTime(#1/1/1973 1:37:25 PM#,4)"
    })
    void testFuncsString(String expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @Test
    void testFuncsQuoting() {
        assertEquals(" FOO \" BAR ", eval("=UCase(\" foo \"\" bar \")"));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "102; Asc(\"foo\")",
            "9786; AscW(\"☺\")",
            "-1; CBool(\"1\")",
            "13; CByte(\"13\")",
            "14; CByte(\"13.7\")",
            "513; CInt(\"513\")",
            "514; CInt(\"513.7\")",
            "345513; CLng(\"345513\")",
            "345514; CLng(\"345513.7\")",
            "-1; IsNull(Null)",
            "0; IsNull(13)",
            "-1; IsDate(#01/02/2003#)",
            "0; IsDate('foo')",
            "0; IsDate('200')",

            "0; IsNumeric(Null)",
            "0; IsNumeric('foo')",
            "0; IsNumeric(#01/02/2003#)",
            "0; IsNumeric('01/02/2003')",
            "-1; IsNumeric(37)",
            "-1; IsNumeric(' 37 ')",
            "-1; IsNumeric(' -37.5e2 ')",
            "-1; IsNumeric(' &H37 ')",
            "0; IsNumeric(' &H37foo ')",
            "0; IsNumeric(' &o39 ')",
            "-1; IsNumeric(' &o36 ')",
            "0; IsNumeric(' &o36.1 ')",

            "1; VarType(Null)",
            "8; VarType('blah')",
            "7; VarType(#01/02/2003#)",
            "3; VarType(42)",
            "5; VarType(CDbl(42))",
            "14; VarType(42.3)",

            "2; InStr('AFOOBAR', 'FOO')",
            "2; InStr('AFOOBAR', 'foo')",
            "2; InStr(1, 'AFOOBAR', 'foo')",
            "0; InStr(1, 'AFOOBAR', 'foo', 0)",
            "2; InStr(1, 'AFOOBAR', 'foo', 1)",
            "2; InStr(1, 'AFOOBAR', 'FOO', 0)",
            "2; InStr(2, 'AFOOBAR', 'FOO')",
            "0; InStr(3, 'AFOOBAR', 'FOO')",
            "0; InStr(17, 'AFOOBAR', 'FOO')",
            "2; InStr(1, 'AFOOBARFOOBAR', 'FOO')",
            "8; InStr(3, 'AFOOBARFOOBAR', 'FOO')",

            "2; InStrRev('AFOOBAR', 'FOO')",
            "2; InStrRev('AFOOBAR', 'foo')",
            "2; InStrRev('AFOOBAR', 'foo', -1)",
            "0; InStrRev('AFOOBAR', 'foo', -1, 0)",
            "2; InStrRev('AFOOBAR', 'foo', -1, 1)",
            "2; InStrRev('AFOOBAR', 'FOO', -1, 0)",
            "2; InStrRev('AFOOBAR', 'FOO', 4)",
            "0; InStrRev('AFOOBAR', 'FOO', 3)",
            "2; InStrRev('AFOOBAR', 'FOO', 17)",
            "2; InStrRev('AFOOBARFOOBAR', 'FOO', 9)",
            "8; InStrRev('AFOOBARFOOBAR', 'FOO', 10)",

            "1; StrComp('FOO', 'bar')",
            "-1; StrComp('bar', 'FOO')",
            "0; StrComp('FOO', 'foo')",
            "-1; StrComp('FOO', 'bar', 0)",
            "1; StrComp('bar', 'FOO', 0)",
            "-1; StrComp('FOO', 'foo', 0)"
    })
    void testFuncsInt(int expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "Str(Null)",
            "InStr(3, Null, 'FOO')",
            "InStrRev(Null, 'FOO', 3)",
            "Choose(-1,'foo','bar','blah')",
            "Switch(False,'foo', False, 'bar', False, 'blah')"
    })
    void testFuncsNull(String exprStr) {
        assertNull(eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "57.12345; CDbl(\"57.12345\")",
            "1615198d; Val('    1615 198th Street N.E.')",
            "-1d; Val('  &HFFFFwhatever')",
            "131071d; Val('  &H1FFFFwhatever')",
            "-1d; Val('  &HFFFFFFFFwhatever')",
            "291d; Val('  &H123whatever')",
            "83d; Val('  &O123whatever')",
            "1.23d; Val('  1 2 3 e -2 whatever')",
            "0d; Val('  whatever123 ')",
            "0d; Val('')"
    })
    void testFuncsDouble(double expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "57.1235; CCur(\"57.12346\")",
            "57.123456789; CDec(\"57.123456789\")"
    })
    void testFuncsBigDecimal(BigDecimal expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "57.12345; CSng(\"57.12345\")"
    })
    void testFuncsFloat(String expected, String exprStr) {
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

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = ';',
        quoteCharacter = '\"',
        value = {
            "12345.6789; Format(12345.6789, 'General Number')",
            "0.12345; Format(0.12345, 'General Number')",
            "-12345.6789; Format(-12345.6789, 'General Number')",
            "-0.12345; Format(-0.12345, 'General Number')",
            "12345.6789; Format('12345.6789', 'General Number')",
            "1678.9; Format('1.6789E+3', 'General Number')",
            "37623.2916666667; Format(#01/02/2003 7:00:00 AM#, 'General Number')",
            "foo; Format('foo', 'General Number')",

            "12,345.68; Format(12345.6789, 'Standard')",
            "0.12; Format(0.12345, 'Standard')",
            "-12,345.68; Format(-12345.6789, 'Standard')",
            "-0.12; Format(-0.12345, 'Standard')",

            "12345.68; Format(12345.6789, 'Fixed')",
            "0.12; Format(0.12345, 'Fixed')",
            "-12345.68; Format(-12345.6789, 'Fixed')",
            "-0.12; Format(-0.12345, 'Fixed')",

            "€12,345.68; Format(12345.6789, 'Euro')",
            "€0.12; Format(0.12345, 'Euro')",
            "(€12,345.68); Format(-12345.6789, 'Euro')",
            "(€0.12); Format(-0.12345, 'Euro')",

            "$12,345.68; Format(12345.6789, 'Currency')",
            "$0.12; Format(0.12345, 'Currency')",
            "($12,345.68); Format(-12345.6789, 'Currency')",
            "($0.12); Format(-0.12345, 'Currency')",

            "1234567.89%; Format(12345.6789, 'Percent')",
            "12.34%; Format(0.12345, 'Percent')",
            "-1234567.89%; Format(-12345.6789, 'Percent')",
            "-12.34%; Format(-0.12345, 'Percent')",

            "1.23E+4; Format(12345.6789, 'Scientific')",
            "1.23E-1; Format(0.12345, 'Scientific')",
            "-1.23E+4; Format(-12345.6789, 'Scientific')",
            "-1.23E-1; Format(-0.12345, 'Scientific')",

            "Yes; Format(True, 'Yes/No')",
            "No; Format(False, 'Yes/No')",
            "True; Format(True, 'True/False')",
            "False; Format(False, 'True/False')",
            "On; Format(True, 'On/Off')",
            "Off; Format(False, 'On/Off')",

            "1/2/2003 7:00:00 AM; Format(#01/02/2003 7:00:00 AM#, 'General Date')",
            "1/2/2003; Format(#01/02/2003#, 'General Date')",
            "7:00:00 AM; Format(#7:00:00 AM#, 'General Date')",
            "1/2/2003 7:00:00 AM; Format('37623.2916666667', 'General Date')",
            "foo; Format('foo', 'General Date')",
            "\"\"; Format('', 'General Date')",

            "Thursday, January 02, 2003; Format(#01/02/2003 7:00:00 AM#, 'Long Date')",
            "02-Jan-03; Format(#01/02/2003 7:00:00 AM#, 'Medium Date')",
            "1/2/2003; Format(#01/02/2003 7:00:00 AM#, 'Short Date')",
            "7:00:00 AM; Format(#01/02/2003 7:00:00 AM#, 'Long Time')",
            "07:00 AM; Format(#01/02/2003 7:00:00 AM#, 'Medium Time')",
            "07:00; Format(#01/02/2003 7:00:00 AM#, 'Short Time')",
            "19:00; Format(#01/02/2003 7:00:00 PM#, 'Short Time')"
    })
    void testFormat(String expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @CsvSource(delimiter = '|',
        quoteCharacter = '§',
        value = {
        "07:00 a| Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p')",
        "07:00 p| Format(#01/10/2003 7:00:00 PM#, 'hh:nn a/p')",
        "07:00 a 6 2| Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p w ww')",
        "07:00 a 4 1| Format(#01/10/2003 7:00:00 AM#, 'hh:nn a/p w ww', 3, 3)",
        "1313| Format(#01/10/2003 7:13:00 AM#, 'nnnn; foo bar')",
        "1 1/10/2003 7:13:00 AM ttt this is text| Format(#01/10/2003 7:13:00 AM#, 'q c ttt \"this is text\"')",
        "1 1/10/2003 ttt this is text| Format(#01/10/2003#, 'q c ttt \"this is text\"')",
        "4 7:13:00 AM ttt this 'is' \"text\"| Format(#7:13:00 AM#, \"q c ttt \"\"this 'is' \"\"\"\"text\"\"\"\"\"\"\")",
        "12/29/1899| Format('true', 'c')",
        "Tuesday, 00 Jan 2, 21:36:00 Y| Format('3.9', '*~dddd, yy mmm d, hh:nn:ss \\Y[Yellow]')",
        "Tuesday, 00 Jan 01/2, 09:36:00 PM| Format('3.9', 'dddd, yy mmm mm/d, hh:nn:ss AMPM')",
        "9:36:00 PM| Format('3.9', 'ttttt')",
        "9:36:00 PM| Format(3.9, 'ttttt')",
        "foo| Format('foo', 'dddd, yy mmm mm d, hh:nn:ss AMPM')"
    })
    void testCustomFormat1(String expected, String exprStr) {
        assertEquals(expected, eval('=' + exprStr));
    }

    @Test
    void testCustomFormat2() {
        assertEvalFormat("';\\y;\\n'",
            "foo", "'foo'",
            "", "''",
            "y", "True",
            "n", "'0'",
            "", "Null");

        assertEvalFormat("'\\p;\"y\";!\\n;*~\\z[Blue];'",
            "foo", "'foo'",
            "", "''",
            "y", "True",
            "n", "'0'",
            "p", "'10'",
            "z", "Null");

        assertEvalFormat("'\"p\"#.00#\"blah\"'",
            "p13.00blah", "13",
            "-p13.00blah", "-13",
            "p.00blah", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'\"p\"#.00#\"blah\";(\"p\"#.00#\"blah\")'",
            "p13.00blah", "13",
            "(p13.00blah)", "-13",
            "p.00blah", "0",
            "(p1.00blah)", "True",
            "p.00blah", "'false'",
            "p37623.292blah", "#01/02/2003 7:00:00 AM#",
            "p37623.292blah", "'01/02/2003 7:00:00 AM'",
            "NotANumber", "'NotANumber'",
            "", "''",
            "", "Null");

        assertEvalFormat("'\"p\"#.00#\"blah\";!(\"p\"#.00#\"blah\")[Red];\"zero\"'",
            "p13.00blah", "13",
            "(p13.00blah)", "-13",
            "zero", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'\\p#.00#\"blah\";*~(\"p\"#.00#\"blah\");\"zero\";\"yuck\"'",
            "p13.00blah", "13",
            "(p13.00blah)", "-13",
            "zero", "0",
            "", "''",
            "yuck", "Null");

        assertEvalFormat("'0.##;(0.###);\"zero\";\"yuck\";'",
            "0.03", "0.03",
            "zero", "0.003",
            "(0.003)", "-0.003",
            "zero", "-0.0003");

        assertEvalFormat("'0.##;(0.###E+0)'",
            "0.03", "0.03",
            "(3.E-4)", "-0.0003",
            "0.", "0",
            "34223.", "34223",
            "(3.422E+4)", "-34223");

        assertEvalFormat("'0.###E-0'",
            "3.E-4", "0.0003",
            "3.422E4", "34223");

        assertEvalFormat("'0.###e+0'",
            "3.e-4", "0.0003",
            "3.422e+4", "34223");

        assertEvalFormat("'0.###e-0'",
            "3.e-4", "0.0003",
            "3.422e4", "34223");

        assertEvalFormat("'#,##0.###'",
            "0.003", "0.003",
            "0.", "0.0003",
            "34,223.", "34223");

        assertEvalFormat("'0.'",
            "13.", "13",
            "0.", "0.003",
            "-45.", "-45",
            "0.", "-0.003",
            "0.", "0");

        assertEvalFormat("'0.#'",
            "13.", "13",
            "0.3", "0.3",
            "0.", "0.003",
            "-45.", "-45",
            "0.", "-0.003",
            "0.", "0");

        assertEvalFormat("'0'",
            "13", "13",
            "0", "0.003",
            "-45", "-45",
            "0", "-0.003",
            "0", "0");

        assertEvalFormat("'%0'",
            "%13", "0.13",
            "%0", "0.003",
            "-%45", "-0.45",
            "%0", "-0.003",
            "%0", "0");

        assertEvalFormat("'#'",
            "13", "13",
            "", "0.003",
            "-45", "-45",
            "", "-0.003",
            "", "0");

        assertEvalFormat("'\\0\\[#.#\\]\\0'",
            "0[13.]0", "13",
            "0[.]0", "0.003",
            "0[.3]0", "0.3",
            "-0[45.]0", "-45",
            "0[.]0", "-0.003",
            "-0[.3]0", "-0.3",
            "0[.]0", "0");

        assertEvalFormat("\"#;n'g;'\"",
            "5", "5",
            "n'g", "-5",
            "'", "0");

        assertEvalFormat("'$0.0#'",
            "$213.0", "213");

        assertEvalFormat("'@'",
            "foo", "'foo'",
            "-13", "-13",
            "0", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'>@'",
            "FOO", "'foo'",
            "-13", "-13",
            "0", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'<@'",
            "foo", "'FOO'",
            "-13", "-13",
            "0", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'!>@;'",
            "O", "'foo'",
            "3", "-13",
            "0", "0",
            "", "''",
            "", "Null");

        assertEvalFormat("'!>*~@[Red];\"empty\";'",
            "O", "'foo'",
            "3", "-13",
            "0", "0",
            "empty", "''",
            "empty", "Null");

        assertEvalFormat("'><@'",
            "fOo", "'fOo'");

        assertEvalFormat("'\\x@@@&&&\\y'",
            "x   fy", "'f'",
            "x   fooy", "'foo'",
            "x foobay", "'fooba'",
            "xfoobarybaz", "'foobarbaz'");

        assertEvalFormat("'!\\x@@@&&&\\y'",
            "xf  y", "'f'",
            "xfooy", "'foo'",
            "xfoobay", "'fooba'",
            "xbarbazy", "'foobarbaz'");

        assertEvalFormat("'\\x&&&@@@\\y'",
            "x  fy", "'f'",
            "xfooy", "'foo'",
            "xfoobay", "'fooba'",
            "xfoobarybaz", "'foobarbaz'");

        assertEvalFormat("'!\\x&&&@@@\\y'",
            "xf   y", "'f'",
            "xfoo   y", "'foo'",
            "xfooba y", "'fooba'",
            "xbarbazy", "'foobarbaz'");
    }

    @Test
    void testNumberFuncs() {
        assertEquals(1, eval("=Abs(1)"));
        assertEquals(1, eval("=Abs(-1)"));
        assertEquals(toBD(1.1), eval("=Abs(-1.1)"));

        assertEquals(Math.atan(0.2), eval("=Atan(0.2)"));
        assertEquals(Math.sin(0.2), eval("=Sin(0.2)"));
        assertEquals(Math.tan(0.2), eval("=Tan(0.2)"));
        assertEquals(Math.cos(0.2), eval("=Cos(0.2)"));

        assertEquals(Math.exp(0.2), eval("=Exp(0.2)"));
        assertEquals(Math.log(0.2), eval("=Log(0.2)"));
        assertEquals(Math.sqrt(4.3), eval("=Sqr(4.3)"));

        assertEquals(3, eval("=Fix(3.5)"));
        assertEquals(4, eval("=Fix(4)"));
        assertEquals(-3, eval("=Fix(-3.5)"));
        assertEquals(-4, eval("=Fix(-4)"));

        assertEquals(1, eval("=Sgn(3.5)"));
        assertEquals(1, eval("=Sgn(4)"));
        assertEquals(-1, eval("=Sgn(-3.5)"));
        assertEquals(-1, eval("=Sgn(-4)"));

        assertEquals(3, eval("=Int(3.5)"));
        assertEquals(4, eval("=Int(4)"));
        assertEquals(-4, eval("=Int(-3.5)"));
        assertEquals(-4, eval("=Int(-4)"));

        assertEquals(toBD(4), eval("=Round(3.7)"));
        assertEquals(4, eval("=Round(4)"));
        assertEquals(toBD(-4), eval("=Round(-3.7)"));
        assertEquals(-4, eval("=Round(-4)"));

        assertEquals(toBD(3.73), eval("=Round(3.7345, 2)"));
        assertEquals(4, eval("=Round(4, 2)"));
        assertEquals(toBD(-3.73), eval("=Round(-3.7345, 2)"));
        assertEquals(-4, eval("=Round(-4, 2)"));
    }

    @Test
    void testDateFuncs() {
        assertEquals("1/2/2003", eval("=CStr(DateValue(#01/02/2003 7:00:00 AM#))"));
        assertEquals("7:00:00 AM", eval("=CStr(TimeValue(#01/02/2003 7:00:00 AM#))"));

        assertEquals("1:10:00 PM", eval("=CStr(#13:10:00#)"));

        assertEquals(2003, eval("=Year(#01/02/2003 7:00:00 AM#)"));
        assertEquals(1, eval("=Month(#01/02/2003 7:00:00 AM#)"));
        assertEquals(2, eval("=Day(#01/02/2003 7:00:00 AM#)"));

        assertEquals(2003, eval("=Year('01/02/2003 7:00:00 AM')"));
        assertEquals(1899, eval("=Year(#7:00:00 AM#)"));
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), eval("=Year('01/02 7:00:00 AM')"));

        assertEquals("January", eval("=MonthName(1)"));
        assertEquals("Feb", eval("=MonthName(2,True)"));
        assertEquals("March", eval("=MonthName(3,False)"));

        assertEquals(7, eval("=Hour(#01/02/2003 7:10:27 AM#)"));
        assertEquals(19, eval("=Hour(#01/02/2003 7:10:27 PM#)"));
        assertEquals(10, eval("=Minute(#01/02/2003 7:10:27 AM#)"));
        assertEquals(27, eval("=Second(#01/02/2003 7:10:27 AM#)"));

        assertEquals(7, eval("=Weekday(#11/22/2003#)"));
        assertEquals(3, eval("=Weekday(#11/22/2003#, 5)"));
        assertEquals(1, eval("=Weekday(#11/22/2003#, 7)"));

        assertEquals("Sunday", eval("=WeekdayName(1)"));
        assertEquals("Sun", eval("=WeekdayName(1,True)"));
        assertEquals("Tuesday", eval("=WeekdayName(1,False,3)"));
        assertEquals("Thu", eval("=WeekdayName(3,True,3)"));

        assertTrue(((String) eval("=CStr(Date())")).matches("\\d{1,2}/\\d{1,2}/\\d{4}"));
        assertTrue(((String) eval("=CStr(Time())")).matches("\\d{1,2}:\\d{2}:\\d{2} (AM|PM)"));

        assertEquals("3:57:34 AM", eval("=CStr(TimeSerial(3,57,34))"));
        assertEquals("3:57:34 PM", eval("=CStr(TimeSerial(15,57,34))"));
        assertEquals("5:45:00 AM", eval("=CStr(TimeSerial(6,-15,0))"));
        assertEquals("12:00:00 AM", eval("=CStr(TimeSerial(0,0,0))"));
        assertEquals("2:00:00 PM", eval("=CStr(TimeSerial(-10,0,0))"));
        assertEquals("6:00:00 AM", eval("=CStr(TimeSerial(30,0,0))"));

        assertEquals("2/12/1969", eval("=CStr(DateSerial(69,2,12))"));
        assertEquals("2/12/2010", eval("=CStr(DateSerial(10,2,12))"));
        assertEquals("7/12/2013", eval("=CStr(DateSerial(2014,-5,12))"));
        assertEquals("8/7/2013", eval("=CStr(DateSerial(2014,-5,38))"));

        assertEquals(1, eval("=DatePart('ww',#01/03/2018#)"));
        assertEquals(2, eval("=DatePart('ww',#01/03/2018#,4)"));
        assertEquals(1, eval("=DatePart('ww',#01/03/2018#,5)"));
        assertEquals(1, eval("=DatePart('ww',#01/03/2018#,4,3)"));
        assertEquals(52, eval("=DatePart('ww',#01/03/2018#,5,3)"));
        assertEquals(1, eval("=DatePart('ww',#01/03/2018#,4,2)"));
        assertEquals(53, eval("=DatePart('ww',#01/03/2018#,5,2)"));
        assertEquals(2003, eval("=DatePart('yyyy',#11/22/2003 5:45:13 AM#)"));
        assertEquals(4, eval("=DatePart('q',#11/22/2003 5:45:13 AM#)"));
        assertEquals(11, eval("=DatePart('m',#11/22/2003 5:45:13 AM#)"));
        assertEquals(326, eval("=DatePart('y',#11/22/2003 5:45:13 AM#)"));
        assertEquals(22, eval("=DatePart('d',#11/22/2003 5:45:13 AM#)"));
        assertEquals(7, eval("=DatePart('w',#11/22/2003 5:45:13 AM#)"));
        assertEquals(3, eval("=DatePart('w',#11/22/2003 5:45:13 AM#, 5)"));
        assertEquals(5, eval("=DatePart('h',#11/22/2003 5:45:13 AM#)"));
        assertEquals(45, eval("=DatePart('n',#11/22/2003 5:45:13 AM#)"));
        assertEquals(13, eval("=DatePart('s',#11/22/2003 5:45:13 AM#)"));

        assertEquals("11/22/2005 5:45:13 AM", eval("CStr(DateAdd('yyyy',2,#11/22/2003 5:45:13 AM#))"));
        assertEquals("2/22/2004 5:45:13 AM", eval("CStr(DateAdd('q',1,#11/22/2003 5:45:13 AM#))"));
        assertEquals("1/22/2004 5:45:13 AM", eval("CStr(DateAdd('m',2,#11/22/2003 5:45:13 AM#))"));
        assertEquals("12/12/2003 5:45:13 AM", eval("CStr(DateAdd('d',20,#11/22/2003 5:45:13 AM#))"));
        assertEquals("12/12/2003 5:45:13 AM", eval("CStr(DateAdd('w',20,#11/22/2003 5:45:13 AM#))"));
        assertEquals("12/12/2003 5:45:13 AM", eval("CStr(DateAdd('y',20,#11/22/2003 5:45:13 AM#))"));
        assertEquals("12/27/2003 5:45:13 AM", eval("CStr(DateAdd('ww',5,#11/22/2003 5:45:13 AM#))"));
        assertEquals("11/22/2003 3:45:13 PM", eval("CStr(DateAdd('h',10,#11/22/2003 5:45:13 AM#))"));
        assertEquals("11/22/2003 6:19:13 AM", eval("CStr(DateAdd('n',34,#11/22/2003 5:45:13 AM#))"));
        assertEquals("11/22/2003 5:46:27 AM", eval("CStr(DateAdd('s',74,#11/22/2003 5:45:13 AM#))"));

        assertEquals("12/12/2003", eval("CStr(DateAdd('d',20,#11/22/2003#))"));
        assertEquals("11/22/2003 10:00:00 AM", eval("CStr(DateAdd('h',10,#11/22/2003#))"));
        assertEquals("11/23/2003", eval("CStr(DateAdd('h',24,#11/22/2003#))"));
        assertEquals("3:45:13 PM", eval("CStr(DateAdd('h',10,#5:45:13 AM#))"));
        assertEquals("12/31/1899 11:45:13 AM", eval("CStr(DateAdd('h',30,#5:45:13 AM#))"));

        assertEquals(0, eval("=DateDiff('yyyy',#10/22/2003#,#11/22/2003#)"));
        assertEquals(4, eval("=DateDiff('yyyy',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-4, eval("=DateDiff('yyyy',#11/22/2007#,#10/22/2003#)"));

        assertEquals(0, eval("=DateDiff('q',#10/22/2003#,#11/22/2003#)"));
        assertEquals(3, eval("=DateDiff('q',#03/01/2003#,#11/22/2003#)"));
        assertEquals(16, eval("=DateDiff('q',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-13, eval("=DateDiff('q',#03/22/2007#,#10/22/2003#)"));

        assertEquals(1, eval("=DateDiff('m',#10/22/2003#,#11/01/2003#)"));
        assertEquals(8, eval("=DateDiff('m',#03/22/2003#,#11/01/2003#)"));
        assertEquals(49, eval("=DateDiff('m',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-41, eval("=DateDiff('m',#03/22/2007#,#10/01/2003#)"));

        assertEquals(10, eval("=DateDiff('d','10/22','11/01')"));
        assertEquals(0, eval("=DateDiff('y',#1:37:00 AM#,#2:15:00 AM#)"));
        assertEquals(10, eval("=DateDiff('d',#10/22/2003#,#11/01/2003#)"));
        assertEquals(1, eval("=DateDiff('d',#10/22/2003 11:00:00 PM#,#10/23/2003 1:00:00 AM#)"));
        assertEquals(224, eval("=DateDiff('d',#03/22/2003#,#11/01/2003#)"));
        assertEquals(1492, eval("=DateDiff('y',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-1268, eval("=DateDiff('d',#03/22/2007#,#10/01/2003#)"));
        assertEquals(366, eval("=DateDiff('d',#1/1/2000#,#1/1/2001#)"));
        assertEquals(365, eval("=DateDiff('d',#1/1/2001#,#1/1/2002#)"));

        assertEquals(0, eval("=DateDiff('w',#11/3/2018#,#11/04/2018#)"));
        assertEquals(1, eval("=DateDiff('w',#11/3/2018#,#11/10/2018#)"));
        assertEquals(0, eval("=DateDiff('w',#12/31/2017#,#1/1/2018#)"));
        assertEquals(32, eval("=DateDiff('w',#03/22/2003#,#11/01/2003#)"));
        assertEquals(213, eval("=DateDiff('w',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-181, eval("=DateDiff('w',#03/22/2007#,#10/01/2003#)"));

        assertEquals(1, eval("=DateDiff('ww',#11/3/2018#,#11/04/2018#)"));
        assertEquals(1, eval("=DateDiff('ww',#11/3/2018#,#11/10/2018#)"));
        assertEquals(0, eval("=DateDiff('ww',#12/31/2017#,#1/1/2018#)"));
        assertEquals(1, eval("=DateDiff('ww',#12/31/2017#,#1/1/2018#,2)"));
        assertEquals(0, eval("=DateDiff('ww',#12/31/2017#,#1/1/2018#,1,3)"));
        assertEquals(53, eval("=DateDiff('ww',#1/1/2000#,#1/1/2001#)"));
        assertEquals(32, eval("=DateDiff('ww',#03/22/2003#,#11/01/2003#)"));
        assertEquals(213, eval("=DateDiff('ww',#10/22/2003#,#11/22/2007#)"));
        assertEquals(-181, eval("=DateDiff('ww',#03/22/2007#,#10/01/2003#)"));

        assertEquals(1, eval("=DateDiff('h',#1:37:00 AM#,#2:15:00 AM#)"));
        assertEquals(13, eval("=DateDiff('h',#1:37:00 AM#,#2:15:00 PM#)"));
        assertEquals(1, eval("=DateDiff('h',#11/3/2018 1:37:00 AM#,#11/3/2018 2:15:00 AM#)"));
        assertEquals(13, eval("=DateDiff('h',#11/3/2018 1:37:00 AM#,#11/3/2018 2:15:00 PM#)"));
        assertEquals(24, eval("=DateDiff('h',#11/3/2018#,#11/4/2018#)"));
        assertEquals(5641, eval("=DateDiff('h',#3/13/2018 1:37:00 AM#,#11/3/2018 2:15:00 AM#)"));
        assertEquals(23161, eval("=DateDiff('h',#3/13/2016 1:37:00 AM#,#11/3/2018 2:15:00 AM#)"));
        assertEquals(-23173, eval("=DateDiff('h',#11/3/2018 2:15:00 PM#,#3/13/2016 1:37:00 AM#)"));

        assertEquals(1, eval("=DateDiff('n',#1:37:59 AM#,#1:38:00 AM#)"));
        assertEquals(758, eval("=DateDiff('n',#1:37:30 AM#,#2:15:13 PM#)"));
        assertEquals(1, eval("=DateDiff('n',#11/3/2018 1:37:59 AM#,#11/3/2018 1:38:00 AM#)"));
        assertEquals(758, eval("=DateDiff('n',#11/3/2018 1:37:59 AM#,#11/3/2018 2:15:00 PM#)"));
        assertEquals(1440, eval("=DateDiff('n',#11/3/2018#,#11/4/2018#)"));
        assertEquals(338438, eval("=DateDiff('n',#3/13/2018 1:37:59 AM#,#11/3/2018 2:15:00 AM#)"));
        assertEquals(1389638, eval("=DateDiff('n',#3/13/2016 1:37:30 AM#,#11/3/2018 2:15:13 AM#)"));
        assertEquals(-1390358, eval("=DateDiff('n',#11/3/2018 2:15:30 PM#,#3/13/2016 1:37:13 AM#)"));

        assertEquals(1, eval("=DateDiff('s',#1:37:59 AM#,#1:38:00 AM#)"));
        assertEquals(35, eval("=DateDiff('s',#1:37:10 AM#,#1:37:45 AM#)"));
        assertEquals(45463, eval("=DateDiff('s',#1:37:30 AM#,#2:15:13 PM#)"));
        assertEquals(1, eval("=DateDiff('s',#11/3/2018 1:37:59 AM#,#11/3/2018 1:38:00 AM#)"));
        assertEquals(45463, eval("=DateDiff('s',#11/3/2018 1:37:30 AM#,#11/3/2018 2:15:13 PM#)"));
        assertEquals(86400, eval("=DateDiff('s',#11/3/2018#,#11/4/2018#)"));
        assertEquals(20306221, eval("=DateDiff('s',#3/13/2018 1:37:59 AM#,#11/3/2018 2:15:00 AM#)"));
        assertEquals(83378263, eval("=DateDiff('s',#3/13/2016 1:37:30 AM#,#11/3/2018 2:15:13 AM#)"));
        assertEquals(-83421497, eval("=DateDiff('s',#11/3/2018 2:15:30 PM#,#3/13/2016 1:37:13 AM#)"));
    }

    @Test
    void testFinancialFuncs() {
        assertEquals("-9.57859403981306", eval("=CStr(NPer(0.12/12,-100,-1000))"));
        assertEquals("-9.48809500550578", eval("=CStr(NPer(0.12/12,-100,-1000,0,1))"));
        assertEquals("60.0821228537616", eval("=CStr(NPer(0.12/12,-100,-1000,10000))"));
        assertEquals("59.6738656742947", eval("=CStr(NPer(0.12/12,-100,-1000,10000,1))"));
        assertEquals("69.6607168935747", eval("=CStr(NPer(0.12/12,-100,0,10000))"));
        assertEquals("69.1619606798005", eval("=CStr(NPer(0.12/12,-100,0,10000,1))"));

        assertEquals("8166.96698564091", eval("=CStr(FV(0.12/12,60,-100))"));
        assertEquals("8248.63665549732", eval("=CStr(FV(0.12/12,60,-100,0,1))"));
        assertEquals("6350.27028707682", eval("=CStr(FV(0.12/12,60,-100,1000))"));
        assertEquals("6431.93995693323", eval("=CStr(FV(0.12/12,60,-100,1000,1))"));

        assertEquals("4495.5038406224", eval("=CStr(PV(0.12/12,60,-100))"));
        assertEquals("4540.45887902863", eval("=CStr(PV(0.12/12,60,-100,0,1))"));
        assertEquals("-1008.99231875519", eval("=CStr(PV(0.12/12,60,-100,10000))"));
        assertEquals("-964.03728034897", eval("=CStr(PV(0.12/12,60,-100,10000,1))"));

        assertEquals("22.2444476849018", eval("=CStr(Pmt(0.12/12,60,-1000))"));
        assertEquals("22.0242056286156", eval("=CStr(Pmt(0.12/12,60,-1000,0,1))"));
        assertEquals("-100.200029164116", eval("=CStr(Pmt(0.12/12,60,-1000,10000))"));
        assertEquals("-99.2079496674414", eval("=CStr(Pmt(0.12/12,60,-1000,10000,1))"));
        assertEquals("-122.444476849018", eval("=CStr(Pmt(0.12/12,60,0,10000))"));
        assertEquals("-121.232155296057", eval("=CStr(Pmt(0.12/12,60,0,10000,1))"));
        assertEquals("22.2444476849018", eval("=CStr(Pmt(0.12/12,60,-1000))"));

        assertEquals("10", eval("=CStr(IPmt(0.12/12,1,60,-1000))"));
        assertEquals("5.90418478297567", eval("=CStr(IPmt(0.12/12,30,60,-1000))"));
        assertEquals("0", eval("=CStr(IPmt(0.12/12,1,60,-1000,0,1))"));
        assertEquals("5.8457275078967", eval("=CStr(IPmt(0.12/12,30,60,-1000,0,1))"));
        assertEquals("0", eval("=CStr(IPmt(0.12/12,1,60,0,10000))"));
        assertEquals("40.9581521702433", eval("=CStr(IPmt(0.12/12,30,60,0,10000))"));
        assertEquals("0", eval("=CStr(IPmt(0.12/12,1,60,0,10000,1))"));
        assertEquals("40.552625911132", eval("=CStr(IPmt(0.12/12,30,60,0,10000,1))"));
        assertEquals("10", eval("=CStr(IPmt(0.12/12,1,60,-1000,10000))"));
        assertEquals("46.862336953219", eval("=CStr(IPmt(0.12/12,30,60,-1000,10000))"));
        assertEquals("0", eval("=CStr(IPmt(0.12/12,1,60,-1000,10000,1))"));
        assertEquals("46.3983534190287", eval("=CStr(IPmt(0.12/12,30,60,-1000,10000,1))"));

        assertEquals("12.2444476849018", eval("=CStr(PPmt(0.12/12,1,60,-1000))"));
        assertEquals("16.3402629019261", eval("=CStr(PPmt(0.12/12,30,60,-1000))"));
        assertEquals("22.0242056286156", eval("=CStr(PPmt(0.12/12,1,60,-1000,0,1))"));
        assertEquals("16.1784781207189", eval("=CStr(PPmt(0.12/12,30,60,-1000,0,1))"));
        assertEquals("-122.444476849018", eval("=CStr(PPmt(0.12/12,1,60,0,10000))"));
        assertEquals("-163.402629019261", eval("=CStr(PPmt(0.12/12,30,60,0,10000))"));
        assertEquals("-121.232155296057", eval("=CStr(PPmt(0.12/12,1,60,0,10000,1))"));
        assertEquals("-161.784781207189", eval("=CStr(PPmt(0.12/12,30,60,0,10000,1))"));
        assertEquals("-110.200029164116", eval("=CStr(PPmt(0.12/12,1,60,-1000,10000))"));
        assertEquals("-147.062366117335", eval("=CStr(PPmt(0.12/12,30,60,-1000,10000))"));
        assertEquals("-99.2079496674414", eval("=CStr(PPmt(0.12/12,1,60,-1000,10000,1))"));
        assertEquals("-145.60630308647", eval("=CStr(PPmt(0.12/12,30,60,-1000,10000,1))"));

        assertEquals("1.31506849315068", eval("=CStr(DDB(2400,300,10*365,1))"));
        assertEquals("40", eval("=CStr(DDB(2400,300,10*12,1))"));
        assertEquals("480", eval("=CStr(DDB(2400,300,10,1))"));
        assertEquals("22.1225472000002", eval("=CStr(DDB(2400,300,10,10))"));
        assertEquals("245.76", eval("=CStr(DDB(2400,300,10,4))"));
        assertEquals("307.2", eval("=CStr(DDB(2400,300,10,3))"));
        assertEquals("480", eval("=CStr(DDB(2400,300,10,0.1))"));
        assertEquals("274.768033075174", eval("=CStr(DDB(2400,300,10,3.5))"));

        assertEquals("2250", eval("=CStr(SLN(30000,7500,10))"));
        assertEquals("1000", eval("=CStr(SLN(10000,5000,5))"));
        assertEquals("1142.85714285714", eval("=CStr(SLN(8000,0,7))"));

        assertEquals("4090.90909090909", eval("=CStr(SYD(30000,7500,10,1))"));
        assertEquals("409.090909090909", eval("=CStr(SYD(30000,7500,10,10))"));

        assertEquals("-1.63048347266756E-02", eval("=CStr(Rate(3,200,-610,0,-20,0.1))"));
        // the result of this varies slightly depending on the jvm impl, so we
        // round it to fewer decimal places (7.70147248820165E-03 or
        // 7.70147248820155E-03)
        assertEquals("7.701472488202E-03", eval("=Format(CStr(Rate(4*12,-200,8000)), '#.############E+00')"));
        assertEquals("-1.09802980531205", eval("=CStr(Rate(60,93.22,5000,0.1))"));
    }

    private static void assertEvalFormat(String fmtStr, String... testStrs) {
        for (int i = 0; i < testStrs.length; i += 2) {
            String expected = testStrs[i];
            String val = testStrs[i + 1];
            assertEquals(expected, eval("=Format(" + val + ", " + fmtStr + ")"), "Input " + val);
        }
    }

}
