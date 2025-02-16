package io.github.spannm.jackcess.expr;

import io.github.spannm.jackcess.impl.expr.FormatUtil;
import io.github.spannm.jackcess.impl.expr.NumberFormatter;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A NumericConfig encapsulates number formatting options for expression evaluation. The default
 * {@link #US_NUMERIC_CONFIG} instance provides US specific locale configuration. Databases which have been built for
 * other locales can utilize custom implementations of NumericConfig in order to evaluate expressions correctly.
 */
public class NumericConfig {
    public static final NumericConfig US_NUMERIC_CONFIG = new NumericConfig(2, true, false, true, 3, Locale.US);

    public enum Type {
        CURRENCY,
        FIXED,
        STANDARD,
        PERCENT,
        SCIENTIFIC,
        EURO
    }

    private final int                  mnumDecDigits;
    private final boolean              mincLeadingDigit;
    private final boolean              museNegParens;
    private final boolean              museNegCurrencyParens;
    private final int                  mnumGroupDigits;
    private final DecimalFormatSymbols msymbols;
    private final NumberFormatter      mnumFmt;
    private final String               mcurrencyFormat;
    private final String               mfixedFormat;
    private final String               mstandardFormat;
    private final String               mpercentFormat;
    private final String               mscientificFormat;
    private final String               meuroFormat;

    public NumericConfig(int numDecDigits, boolean incLeadingDigit, boolean useNegParens, boolean useNegCurrencyParens, int numGroupDigits, Locale locale) {
        mnumDecDigits = numDecDigits;
        mincLeadingDigit = incLeadingDigit;
        museNegParens = useNegParens;
        museNegCurrencyParens = useNegCurrencyParens;
        mnumGroupDigits = numGroupDigits;
        msymbols = DecimalFormatSymbols.getInstance(locale);
        mnumFmt = new NumberFormatter(msymbols);

        mcurrencyFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.CURRENCY, mnumDecDigits, mincLeadingDigit, museNegCurrencyParens, mnumGroupDigits);
        mfixedFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.GENERAL, mnumDecDigits, true, museNegParens, 0);
        mstandardFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.GENERAL, mnumDecDigits, mincLeadingDigit, museNegParens, mnumGroupDigits);
        mpercentFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.PERCENT, mnumDecDigits, mincLeadingDigit, museNegParens, 0);
        mscientificFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.SCIENTIFIC, mnumDecDigits, true, false, 0);
        meuroFormat = FormatUtil.createNumberFormatPattern(FormatUtil.NumPatternType.EURO, mnumDecDigits, mincLeadingDigit, museNegCurrencyParens, mnumGroupDigits);
    }

    public int getNumDecimalDigits() {
        return mnumDecDigits;
    }

    public boolean includeLeadingDigit() {
        return mincLeadingDigit;
    }

    public boolean useParensForNegatives() {
        return museNegParens;
    }

    public boolean useParensForCurrencyNegatives() {
        return museNegCurrencyParens;
    }

    public int getNumGroupingDigits() {
        return mnumGroupDigits;
    }

    public String getNumberFormat(Type type) {
        switch (type) {
            case CURRENCY:
                return mcurrencyFormat;
            case FIXED:
                return mfixedFormat;
            case STANDARD:
                return mstandardFormat;
            case PERCENT:
                return mpercentFormat;
            case SCIENTIFIC:
                return mscientificFormat;
            case EURO:
                return meuroFormat;
            default:
                throw new IllegalArgumentException("unknown number type " + type);
        }
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return msymbols;
    }

    /**
     * @return the given float formatted according to the current locale config
     */
    public String format(float f) {
        return mnumFmt.format(f);
    }

    /**
     * @return the given double formatted according to the current locale config
     */
    public String format(double d) {
        return mnumFmt.format(d);
    }

    /**
     * @return the given BigDecimal formatted according to the current locale config
     */
    public String format(BigDecimal bd) {
        return mnumFmt.format(bd);
    }
}
