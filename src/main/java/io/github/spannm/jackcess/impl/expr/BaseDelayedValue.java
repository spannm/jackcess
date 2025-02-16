package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class BaseDelayedValue implements Value {
    private Value val;

    protected BaseDelayedValue() {
    }

    private Value getDelegate() {
        if (val == null) {
            val = eval();
        }
        return val;
    }

    @Override
    public boolean isNull() {
        return getType() == Type.NULL;
    }

    @Override
    public Value.Type getType() {
        return getDelegate().getType();
    }

    @Override
    public Object get() {
        return getDelegate().get();
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        return getDelegate().getAsBoolean(ctx);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return getDelegate().getAsString(ctx);
    }

    @Override
    public LocalDateTime getAsLocalDateTime(LocaleContext ctx) {
        return getDelegate().getAsLocalDateTime(ctx);
    }

    @Override
    public Value getAsDateTimeValue(LocaleContext ctx) {
        return getDelegate().getAsDateTimeValue(ctx);
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return getDelegate().getAsLongInt(ctx);
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return getDelegate().getAsDouble(ctx);
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return getDelegate().getAsBigDecimal(ctx);
    }

    protected abstract Value eval();
}
