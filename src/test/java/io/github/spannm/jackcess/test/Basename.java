package io.github.spannm.jackcess.test;

/**
 * Defines known valid database test file base names.
 */
public enum Basename {

    BIG_INDEX("bigIndexTest"),
    BINARY_INDEX("binIdxTest"),
    BLOB("testOle"),
    CALC_FIELD("calcFieldTest"),
    COMPLEX("complexDataTest"),
    COMP_INDEX("compIndexTest"),
    DEL("delTest"),
    DEL_COL("delColTest"),
    EXT_DATE("extDateTest"),
    FIXED_NUMERIC("fixedNumericTest"),
    FIXED_TEXT("fixedTextTest"),
    INDEX("indexTest"),
    INDEX_CODES("testIndexCodes"),
    INDEX_CURSOR("indexCursorTest"),
    INDEX_PROPERTIES("testIndexProperties"),
    LINKED("linkerTest"),
    LINKED_ODBC("odbcLinkerTest"),
    OLD_DATES("oldDates"),
    OVERFLOW("overflowTest"),
    PROMOTION("testPromotion"),
    QUERY("queryTest"),
    TEST("test"),
    TEST2("test2"),
    UNSUPPORTED("unsupportedFieldsTest");

    private final String basename;

    Basename(String _basename) {
        basename = _basename;
    }

    @Override
    public String toString() {
        return basename;
    }

}
