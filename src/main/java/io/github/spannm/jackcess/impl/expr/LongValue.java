package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class LongValue extends BaseNumericValue {
    private final Integer val;

    public LongValue(Integer _val) {
        val = _val;
    }

    @Override
    public Type getType() {
        return Type.LONG;
    }

    @Override
    public Object get() {
        return val;
    }

    @Override
    protected Number getNumber() {
        return val;
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        return val.longValue() != 0L;
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return val;
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(val);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return val.toString();
    }
}
