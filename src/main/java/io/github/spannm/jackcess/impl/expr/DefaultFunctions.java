package io.github.spannm.jackcess.impl.expr;

import static io.github.spannm.jackcess.impl.expr.FunctionSupport.getOptionalIntParam;

import io.github.spannm.jackcess.expr.*;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.expr.FunctionSupport.*; // NOPMD

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class DefaultFunctions {
    private static final Map<String, Function> FUNCS = new HashMap<>();

    static {
        // load all default functions
        Logger logger = System.getLogger(DefaultFunctions.class.getName());
        for (Object obj : List.of(
            new DefaultTextFunctions(),
            new DefaultNumberFunctions(),
            new DefaultDateFunctions(),
            new DefaultFinancialFunctions())) {
            logger.log(Level.DEBUG, "Loaded functions from " + obj.getClass().getName());
        }
    }

    public static final FunctionLookup LOOKUP = name -> FUNCS.get(DatabaseImpl.toLookupName(name));

    public static final Function IIF    = registerFunc(new Func3("IIf") {
        @Override
        protected Value eval3(EvalContext ctx,
            Value param1, Value param2, Value param3) {
            // null is false
            return !param1.isNull() && param1.getAsBoolean(ctx) ? param2 : param3;
        }
    });

    public static final Function HEX    = registerStringFunc(new Func1NullIsNull("Hex") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            if (param1.getType().isString() && param1.getAsString(ctx).isEmpty()) {
                return ValueSupport.ZERO_VAL;
            }
            int lv = param1.getAsLongInt(ctx);
            return ValueSupport.toValue(Integer.toHexString(lv).toUpperCase());
        }
    });

    public static final Function NZ     = registerFunc(new FuncVar("Nz", 1, 2) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            Value param1 = params[0];
            if (!param1.isNull()) {
                return param1;
            }
            if (params.length > 1) {
                return params[1];
            }
            Value.Type resultType = ctx.getResultType();
            return resultType == null || resultType.isString() ? ValueSupport.EMPTY_STR_VAL : ValueSupport.ZERO_VAL;
        }
    });

    public static final Function CHOOSE = registerFunc(new FuncVar("Choose", 1, Integer.MAX_VALUE) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            Value param1 = params[0];
            int idx = param1.getAsLongInt(ctx);
            if (idx < 1 || idx >= params.length) {
                return ValueSupport.NULL_VAL;
            }
            return params[idx];
        }
    });

    public static final Function SWITCH = registerFunc(new FuncVar("Switch") {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            if (params.length % 2 != 0) {
                throw new EvalException("Odd number of parameters");
            }
            for (int i = 0; i < params.length; i += 2) {
                if (params[i].getAsBoolean(ctx)) {
                    return params[i + 1];
                }
            }
            return ValueSupport.NULL_VAL;
        }
    });

    public static final Function OCT    = registerStringFunc(new Func1NullIsNull("Oct") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            if (param1.getType().isString() && param1.getAsString(ctx).isEmpty()) {
                return ValueSupport.ZERO_VAL;
            }
            int lv = param1.getAsLongInt(ctx);
            return ValueSupport.toValue(Integer.toOctalString(lv));
        }
    });

    public static final Function CBOOL  = registerFunc(new Func1("CBool") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            boolean b = param1.getAsBoolean(ctx);
            return ValueSupport.toValue(b);
        }
    });

    public static final Function CBYTE  = registerFunc(new Func1("CByte") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            int lv = param1.getAsLongInt(ctx);
            if (lv < 0 || lv > 255) {
                throw new EvalException("Byte code '" + lv + "' out of range ");
            }
            return ValueSupport.toValue(lv);
        }
    });

    public static final Function CCUR   = registerFunc(new Func1("CCur") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            BigDecimal bd = param1.getAsBigDecimal(ctx);
            bd = bd.setScale(4, NumberFormatter.ROUND_MODE);
            return ValueSupport.toValue(bd);
        }
    });

    public static final Function CDATE  = registerFunc(new Func1("CDate") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            return param1.getAsDateTimeValue(ctx);
        }
    });

    static {
        registerFunc("CVDate", CDATE);
    }

    public static final Function CDBL           = registerFunc(new Func1("CDbl") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            Double dv = param1.getAsDouble(ctx);
            return ValueSupport.toValue(dv);
        }
    });

    public static final Function CDEC           = registerFunc(new Func1("CDec") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            BigDecimal bd = param1.getAsBigDecimal(ctx);
            return ValueSupport.toValue(bd);
        }
    });

    public static final Function CINT           = registerFunc(new Func1("CInt") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            int lv = param1.getAsLongInt(ctx);
            if (lv < Short.MIN_VALUE || lv > Short.MAX_VALUE) {
                throw new EvalException("Int value '" + lv + "' out of range ");
            }
            return ValueSupport.toValue(lv);
        }
    });

    public static final Function CLNG           = registerFunc(new Func1("CLng") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            int lv = param1.getAsLongInt(ctx);
            return ValueSupport.toValue(lv);
        }
    });

    public static final Function CSNG           = registerFunc(new Func1("CSng") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            Double dv = param1.getAsDouble(ctx);
            if (dv < Float.MIN_VALUE || dv > Float.MAX_VALUE) {
                throw new EvalException("Single value '" + dv + "' out of range ");
            }
            return ValueSupport.toValue(dv.floatValue());
        }
    });

    public static final Function CSTR           = registerFunc(new Func1("CStr") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            return ValueSupport.toValue(param1.getAsString(ctx));
        }
    });

    public static final Function CVAR           = registerFunc(new Func1("CVar") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            return param1;
        }
    });

    public static final Function ISNULL         = registerFunc(new Func1("IsNull") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            return ValueSupport.toValue(param1.isNull());
        }
    });

    public static final Function ISDATE         = registerFunc(new Func1("IsDate") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            // for the purposes of this method, a string literal should only
            // return true if it is explicitly a date/time, not if it is
            // just a
            // number (even though casting a number string to a date/time
            // works in
            // general)
            if (param1.getType().isTemporal() || (param1.getType().isString()
                && !stringIsNumeric(ctx, param1)
                && stringIsTemporal(ctx, param1))) {
                return ValueSupport.TRUE_VAL;
            }

            return ValueSupport.FALSE_VAL;
        }
    });

    public static final Function ISNUMERIC      = registerFunc(new Func1("IsNumeric") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            // note, only a string can be considered numeric for this function,
            // even though a date/time can be cast to a number in general
            if (param1.getType().isNumeric() || (param1.getType().isString() && stringIsNumeric(ctx, param1))) {
                return ValueSupport.TRUE_VAL;
            }

            return ValueSupport.FALSE_VAL;
        }
    });

    public static final Function FORMATNUMBER   = registerFunc(new FuncVar("FormatNumber", 1, 6) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            return formatNumber(ctx, params, FormatUtil.NumPatternType.GENERAL);
        }
    });

    public static final Function FORMATPERCENT  = registerFunc(new FuncVar("FormatPercent", 1, 6) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            return formatNumber(ctx, params, FormatUtil.NumPatternType.PERCENT);
        }
    });

    public static final Function FORMATCURRENCY = registerFunc(new FuncVar("FormatCurrency", 1, 6) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            return formatNumber(ctx, params, FormatUtil.NumPatternType.CURRENCY);
        }
    });

    public static final Function FORMATDATETIME = registerFunc(new FuncVar("FormatDateTime", 1, 2) {
        @Override
        protected Value evalVar(EvalContext ctx, Value[] params) {
            Value param1 = params[0];
            if (param1.isNull()) {
                return ValueSupport.NULL_VAL;
            }

            LocalDateTime ldt = param1.getAsLocalDateTime(ctx);

            int fmtType = getOptionalIntParam(ctx, params, 1, 0);
            TemporalConfig.Type tempType = null;
            switch (fmtType) {
                case 0:
                    // vbGeneralDate
                    Value.Type valType = ValueSupport.getDateTimeType(ldt);
                    switch (valType) {
                        case DATE:
                            tempType = TemporalConfig.Type.SHORT_DATE;
                            break;
                        case TIME:
                            tempType = TemporalConfig.Type.LONG_TIME;
                            break;
                        default:
                            tempType = TemporalConfig.Type.GENERAL_DATE;
                    }
                    break;
                case 1:
                    // vbLongDate
                    tempType = TemporalConfig.Type.LONG_DATE;
                    break;
                case 2:
                    // vbShortDate
                    tempType = TemporalConfig.Type.SHORT_DATE;
                    break;
                case 3:
                    // vbLongTime
                    tempType = TemporalConfig.Type.LONG_TIME;
                    break;
                case 4:
                    // vbShortTime
                    tempType = TemporalConfig.Type.SHORT_TIME;
                    break;
                default:
                    throw new EvalException("Unknown format " + fmtType);
            }

            DateTimeFormatter dtf = ctx.createDateFormatter(
                ctx.getTemporalConfig().getDateTimeFormat(tempType));
            return ValueSupport.toValue(dtf.format(ldt));
        }
    });

    public static final Function VARTYPE        = registerFunc(new Func1("VarType") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            Value.Type type = param1.getType();
            int vType = 0;
            switch (type) {
                case NULL:
                    // vbNull
                    vType = 1;
                    break;
                case STRING:
                    // vbString
                    vType = 8;
                    break;
                case DATE:
                case TIME:
                case DATE_TIME:
                    // vbDate
                    vType = 7;
                    break;
                case LONG:
                    // vbLong
                    vType = 3;
                    break;
                case DOUBLE:
                    // vbDouble
                    vType = 5;
                    break;
                case BIG_DEC:
                    // vbDecimal
                    vType = 14;
                    break;
                default:
                    throw new EvalException("Unknown type " + type);
            }
            return ValueSupport.toValue(vType);
        }
    });

    public static final Function TYPENAME       = registerFunc(new Func1("TypeName") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {
            Value.Type type = param1.getType();
            String tName = null;
            switch (type) {
                case NULL:
                    tName = "Null";
                    break;
                case STRING:
                    tName = "String";
                    break;
                case DATE:
                case TIME:
                case DATE_TIME:
                    tName = "Date";
                    break;
                case LONG:
                    tName = "Long";
                    break;
                case DOUBLE:
                    tName = "Double";
                    break;
                case BIG_DEC:
                    tName = "Decimal";
                    break;
                default:
                    throw new EvalException("Unknown type " + type);
            }
            return ValueSupport.toValue(tName);
        }
    });

    public static final Function VAL            = registerStringFunc(new Func1NullIsNull("Val") {
        @Override
        protected Value eval1(EvalContext ctx, Value param1) {

            // strip all whitespace from string
            String str = ValueSupport.WHITESPACE_PAT.matcher(param1.getAsString(ctx))
                .replaceAll("");

            if (str.isEmpty()) {
                return ValueSupport.ZERO_D_VAL;
            }

            Matcher m = null;

            if (str.charAt(0) == ValueSupport.NUMBER_BASE_PREFIX) {

                // see if we can parse as a radix format
                BigInteger bi = null;
                if ((m = ValueSupport.HEX_PAT.matcher(str)).find()) {
                    bi = ValueSupport.parseIntegerString(m.group(), 16);
                } else if ((m = ValueSupport.OCTAL_PAT.matcher(str)).find()) {
                    bi = ValueSupport.parseIntegerString(m.group(), 8);
                }

                if (bi != null) {
                    // this function works differently than normal string to
                    // number
                    // conversion. it seems to coerce these values to a
                    // short/long int
                    // depending on the size of the number (which creates
                    // positive/negative values dependent on the value
                    // length)
                    int iVal = bi.bitLength() <= 16 ? bi.shortValue() : bi.intValue();
                    return ValueSupport.toValue((double) iVal);
                }

            } else {

                // parse as normal "decimal" number.
                if ((m = ValueSupport.NUMBER_PAT.matcher(str)).find()) {
                    BigDecimal bd = new BigDecimal(m.group());
                    return ValueSupport.toValue(bd.doubleValue());
                }
            }

            return ValueSupport.ZERO_D_VAL;
        }
    });

    private DefaultFunctions() {
    }

    private static boolean stringIsNumeric(LocaleContext ctx, Value param) {
        return maybeGetAsBigDecimal(ctx, param) != null;
    }

    static BigDecimal maybeGetAsBigDecimal(LocaleContext ctx, Value param) {
        try {
            return param.getAsBigDecimal(ctx);
        } catch (EvalException ignored) {
            // not a number
        }
        return null;
    }

    private static boolean stringIsTemporal(EvalContext ctx, Value param) {
        return maybeGetAsDateTimeValue(ctx, param) != null;
    }

    static Value maybeGetAsDateTimeValue(LocaleContext ctx, Value param) {
        try {
            // see if we can coerce to date/time
            return param.getAsDateTimeValue(ctx);
        } catch (EvalException ignored) {
            // not a date/time
        }
        return null;
    }

    private static boolean getOptionalTriStateBoolean(EvalContext ctx, Value[] params, int idx, boolean defValue) {
        boolean bv = defValue;
        if (params.length > idx) {
            int val = params[idx].getAsLongInt(ctx);
            switch (val) {
                case 0:
                    // vbFalse
                    bv = false;
                    break;
                case -1:
                    // vbTrue
                    bv = true;
                    break;
                case -2:
                    // vbUseDefault
                    bv = defValue;
                    break;
                default:
                    throw new EvalException("Unsupported tri-state boolean value " + val);
            }
        }
        return bv;
    }

    private static Value formatNumber(EvalContext ctx, Value[] params, FormatUtil.NumPatternType numPatType) {

        Value param1 = params[0];
        if (param1.isNull()) {
            return ValueSupport.NULL_VAL;
        }

        NumericConfig cfg = ctx.getNumericConfig();
        int numDecDigits = getOptionalIntParam(ctx, params, 1, cfg.getNumDecimalDigits(), -1);
        boolean incLeadDigit = getOptionalTriStateBoolean(ctx, params, 2, cfg.includeLeadingDigit());
        boolean defNegParens = numPatType.useParensForNegatives(cfg);
        boolean negParens = getOptionalTriStateBoolean(ctx, params, 3, defNegParens);
        int defNumGroupDigits = cfg.getNumGroupingDigits();
        boolean groupDigits = getOptionalTriStateBoolean(ctx, params, 4, defNumGroupDigits > 0);
        int numGroupDigits = groupDigits ? defNumGroupDigits : 0;

        String fmtStr = FormatUtil.createNumberFormatPattern(numPatType, numDecDigits, incLeadDigit, negParens, numGroupDigits);

        DecimalFormat df = ctx.createDecimalFormat(fmtStr);

        return ValueSupport.toValue(df.format(param1.getAsBigDecimal(ctx)));
    }

    // https://www.techonthenet.com/access/functions/
    // https://support.office.com/en-us/article/Access-Functions-by-category-b8b136c3-2716-4d39-94a2-658ce330ed83

    static Function registerFunc(Function _func) {
        return registerFunc(_func.getName(), _func);
    }

    static Function registerStringFunc(Function _func) {
        registerFunc(_func.getName(), _func);
        registerFunc(new StringFuncWrapper(_func));
        return _func;
    }

    private static Function registerFunc(String _fname, Function _func) {
        System.getLogger(DefaultFunctions.class.getName()).log(Level.TRACE, "Registering function {0}", _fname);
        String lookupFname = DatabaseImpl.toLookupName(_fname);
        if (FUNCS.put(lookupFname, _func) != null) {
            throw new IllegalStateException("Duplicate function " + _fname);
        }
        return _func;
    }
}
