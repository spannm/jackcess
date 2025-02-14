package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class BaseValue implements Value {
    @Override
    public boolean isNull() {
        return getType() == Type.NULL;
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        throw invalidConversion(Type.LONG);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        throw invalidConversion(Type.STRING);
    }

    @Override
    public LocalDateTime getAsLocalDateTime(LocaleContext ctx) {
        return (LocalDateTime) getAsDateTimeValue(ctx).get();
    }

    @Override
    public Value getAsDateTimeValue(LocaleContext ctx) {
        throw invalidConversion(Type.DATE_TIME);
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        throw invalidConversion(Type.LONG);
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        throw invalidConversion(Type.DOUBLE);
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        throw invalidConversion(Type.BIG_DEC);
    }

    protected EvalException invalidConversion(Type newType) {
        return new EvalException(
            this + " cannot be converted to " + newType);
    }

    protected Integer roundToLongInt(LocaleContext ctx) {
        return getAsBigDecimal(ctx).setScale(0, NumberFormatter.ROUND_MODE)
            .intValueExact();
    }

    @Override
    public String toString() {
        return "Value[" + getType() + "] '" + get() + "'";
    }
}
