package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class DoubleValue extends BaseNumericValue {
    private final Double _val;

    public DoubleValue(Double val) {
        _val = val;
    }

    @Override
    public Type getType() {
        return Type.DOUBLE;
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
        return _val != 0.0d;
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return _val;
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(_val);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ctx.getNumericConfig().format(_val);
    }
}
