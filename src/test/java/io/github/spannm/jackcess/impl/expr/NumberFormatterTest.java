package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.NumericConfig;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

class NumberFormatterTest extends AbstractBaseTest {
    private final NumberFormatter numFmt = new NumberFormatter(NumericConfig.US_NUMERIC_CONFIG.getDecimalFormatSymbols());

    static Stream<Arguments> getDoubleFormatData() {
        return Stream.of(
            args("894984737284944", 894984737284944d),
            args("-894984737284944", -894984737284944d),
            args("8949.84737284944", 8949.84737284944d),
            args("8949847372844", 8949847372844d),
            args("8949.847384944", 8949.847384944d),
            args("8.94985647372849E+16", 89498564737284944d),
            args("-8.94985647372849E+16", -89498564737284944d),
            args("895649.847372849", 895649.84737284944d),
            args("300", 300d),
            args("-300", -300d),
            args("0.3", 0.3d),
            args("0.1", 0.1d),
            args("2.3423421E-12", 0.0000000000023423421d),
            args("2.3423421E-11", 0.000000000023423421d),
            args("2.3423421E-10", 0.00000000023423421d),
            args("-2.3423421E-10", -0.00000000023423421d),
            args("2.34234214E-12", 0.00000000000234234214d),
            args("2.342342156E-12", 0.000000000002342342156d),
            args("0.000000023423421", 0.000000023423421d),
            args("2.342342133E-07", 0.0000002342342133d),
            args("1.#INF", Double.POSITIVE_INFINITY),
            args("-1.#INF", Double.NEGATIVE_INFINITY),
            args("1.#QNAN", Double.NaN)
        );
    }
    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @MethodSource("getDoubleFormatData")
    void testDoubleFormat(String _expected, Double _input) {
        assertEquals(_expected, numFmt.format(_input));
    }

    static Stream<Arguments> getFloatFormatData() {
        return Stream.of(
            args("8949847", 8949847f),
            args("-8949847", -8949847f),
            args("8949.847", 8949.847f),
            args("894984", 894984f),
            args("8949.84", 8949.84f),
            args("8.949856E+16", 89498564737284944f),
            args("-8.949856E+16", -89498564737284944f),
            args("895649.9", 895649.84737284944f),
            args("300", 300f),
            args("-300", -300f),
            args("0.3", 0.3f),
            args("0.1", 0.1f),
            args("2.342342E-12", 0.0000000000023423421f),
            args("2.342342E-11", 0.000000000023423421f),
            args("2.342342E-10", 0.00000000023423421f),
            args("-2.342342E-10", -0.00000000023423421f),
            args("2.342342E-12", 0.00000000000234234214f),
            args("2.342342E-12", 0.000000000002342342156f),
            args("0.0000234", 0.0000234f),
            args("2.342E-05", 0.00002342f),
            args("1.#INF", Float.POSITIVE_INFINITY),
            args("-1.#INF", Float.NEGATIVE_INFINITY),
            args("1.#QNAN", Float.NaN)
        );
    }
    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @MethodSource("getFloatFormatData")
    void testFloatFormat(String _expected, Float _input) {
        assertEquals(_expected, numFmt.format(_input));
    }

    static Stream<Arguments> getBigDecimalFormatData() {
        return Stream.of(
            args("9874539485972.2342342234234", "9874539485972.2342342234234"),
            args("9874539485972.234234223423468", "9874539485972.2342342234234678"),
            args("-9874539485972.234234223423468", "-9874539485972.2342342234234678"),
            args("9.874539485972234234223423468E+31", "98745394859722342342234234678000"),
            args("9.874539485972234234223423468E+31", "98745394859722342342234234678000"),
            args("-9.874539485972234234223423468E+31", "-98745394859722342342234234678000"),
            args("300", "300.0"),
            args("-300", "-300.000"),
            args("0.3", "0.3"),
            args("0.1", "0.1000"),
            args("0.0000000000023423428930458", "0.0000000000023423428930458"),
            args("2.3423428930458389038451E-12", "0.0000000000023423428930458389038451"),
            args("2.342342893045838903845134766E-12", "0.0000000000023423428930458389038451347656")
        );
    }
    @ParameterizedTest(name = "[{index}] {1} --> {0}")
    @MethodSource("getBigDecimalFormatData")
    void testDecimalFormat(String _expected, String _input) {
        assertEquals(_expected, numFmt.format(new BigDecimal(_input)));
    }

}
