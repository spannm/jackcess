package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.util.StringUtil;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;

public class StringValue extends BaseValue {
    private static final Object NOT_A_NUMBER = new Object();

    private final String        val;
    private Object              num;

    public StringValue(String _val) {
        val = _val;
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }

    @Override
    public Object get() {
        return val;
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        // ms access seems to treat strings as "true"
        return true;
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return val;
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return roundToLongInt(ctx);
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return getNumber(ctx).doubleValue();
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return getNumber(ctx);
    }

    @Override
    public Value getAsDateTimeValue(LocaleContext ctx) {
        Value dateValue = DefaultDateFunctions.stringToDateValue(ctx, val);

        if (dateValue == null) {
            // see if string can be coerced to number and then to value date (note,
            // numberToDateValue may return null for out of range numbers)
            try {
                dateValue = DefaultDateFunctions.numberToDateValue(getNumber(ctx).doubleValue());
            } catch (EvalException ignored) {
                // not a number, not a date/time
            }

            if (dateValue == null) {
                throw invalidConversion(Type.DATE_TIME);
            }
        }

        // TODO, for now, we can't cache the date value becuase it could be an
        // "implicit" date which would need to be re-calculated on each call
        return dateValue;
    }

    protected BigDecimal getNumber(LocaleContext ctx) {
        if (num instanceof BigDecimal) {
            return (BigDecimal) num;
        }
        if (num == null) {
            // see if it is parseable as a number
            try {
                // ignore extraneous whitespace whitespace and handle "&[hH]" or
                // "&[oO]" prefix (only supports integers)
                String tmpVal = val.trim();
                if (!tmpVal.isEmpty()) {

                    if (tmpVal.charAt(0) != ValueSupport.NUMBER_BASE_PREFIX) {
                        // convert to standard numeric support for parsing
                        tmpVal = toCanonicalNumberFormat(ctx, tmpVal);
                        num = ValueSupport.normalize(new BigDecimal(tmpVal));
                        return (BigDecimal) num;
                    }

                    // parse as hex/octal symbolic value
                    if (ValueSupport.HEX_PAT.matcher(tmpVal).matches()) {
                        return parseIntegerString(tmpVal, 16);
                    } else if (ValueSupport.OCTAL_PAT.matcher(tmpVal).matches()) {
                        return parseIntegerString(tmpVal, 8);
                    }

                    // fall through to NaN
                }
            } catch (NumberFormatException _ex) {
                // fall through to NaN...
            }
            num = NOT_A_NUMBER;
        }
        throw invalidConversion(Type.DOUBLE);
    }

    private BigDecimal parseIntegerString(String tmpVal, int radix) {
        num = new BigDecimal(ValueSupport.parseIntegerString(tmpVal, radix));
        return (BigDecimal) num;
    }

    private static String toCanonicalNumberFormat(LocaleContext ctx, String tmpVal) {
        // convert to standard numeric format:
        // - discard any grouping separators
        // - convert decimal separator to '.'
        DecimalFormatSymbols syms = ctx.getNumericConfig().getDecimalFormatSymbols();
        char groupSepChar = syms.getGroupingSeparator();
        tmpVal = StringUtil.remove(tmpVal, String.valueOf(groupSepChar));

        char decSepChar = syms.getDecimalSeparator();
        if (decSepChar != ValueSupport.CANON_DEC_SEP && tmpVal.indexOf(decSepChar) >= 0) {
            tmpVal = tmpVal.replace(decSepChar, ValueSupport.CANON_DEC_SEP);
        }

        return tmpVal;
    }
}
