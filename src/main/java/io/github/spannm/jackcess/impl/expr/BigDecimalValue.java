package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class BigDecimalValue extends BaseNumericValue {
    private final BigDecimal _val;

    public BigDecimalValue(BigDecimal val) {
        _val = val;
    }

    @Override
    public Type getType() {
        return Type.BIG_DEC;
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
        return _val.compareTo(BigDecimal.ZERO) != 0L;
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ctx.getNumericConfig().format(_val);
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return _val;
    }
}
