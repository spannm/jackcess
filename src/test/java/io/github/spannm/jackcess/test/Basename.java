package io.github.spannm.jackcess.test;

/**
 * Defines known valid database test file base names.
 */
public enum Basename {

    BIG_INDEX("bigIndexTest"),
    COMP_INDEX("compIndexTest"),
    DEL_COL("delColTest"),
    DEL("delTest"),
    FIXED_NUMERIC("fixedNumericTest"),
    FIXED_TEXT("fixedTextTest"),
    INDEX_CURSOR("indexCursorTest"),
    INDEX("indexTest"),
    OVERFLOW("overflowTest"),
    QUERY("queryTest"),
    TEST("test"),
    TEST2("test2"),
    INDEX_CODES("testIndexCodes"),
    INDEX_PROPERTIES("testIndexProperties"),
    PROMOTION("testPromotion"),
    COMPLEX("complexDataTest"),
    UNSUPPORTED("unsupportedFieldsTest"),
    LINKED("linkerTest"),
    LINKED_ODBC("odbcLinkerTest"),
    BLOB("testOle"),
    CALC_FIELD("calcFieldTest"),
    BINARY_INDEX("binIdxTest"),
    OLD_DATES("oldDates"),
    EXT_DATE("extDateTest");

    private final String basename;

    Basename(String _basename) {
        basename = _basename;
    }

    @Override
    public String toString() {
        return basename;
    }

}
