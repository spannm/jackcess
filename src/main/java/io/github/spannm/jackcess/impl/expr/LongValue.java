package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class LongValue extends BaseNumericValue {
    private final Integer _val;

    public LongValue(Integer val) {
        _val = val;
    }

    @Override
    public Type getType() {
        return Type.LONG;
    }

    @Override
    public Object get() {
        return _val;
    }

    @Override
    protected Number getNumber() {
        return _val;
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        return _val.longValue() != 0L;
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return _val;
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(_val);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return _val.toString();
    }
}
