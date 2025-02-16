package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class BigDecimalValue extends BaseNumericValue {
    private final BigDecimal val;

    public BigDecimalValue(BigDecimal _val) {
        val = _val;
    }

    @Override
    public Type getType() {
        return Type.BIG_DEC;
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
        return val.compareTo(BigDecimal.ZERO) != 0L;
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ctx.getNumericConfig().format(val);
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return val;
    }
}
