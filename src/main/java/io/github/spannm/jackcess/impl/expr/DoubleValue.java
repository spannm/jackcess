package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class DoubleValue extends BaseNumericValue {
    private final Double val;

    public DoubleValue(Double _val) {
        val = _val;
    }

    @Override
    public Type getType() {
        return Type.DOUBLE;
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
        return val != 0.0d;
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return val;
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(val);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ctx.getNumericConfig().format(val);
    }
}
