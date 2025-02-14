package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;

public abstract class BaseNumericValue extends BaseValue {

    protected BaseNumericValue() {
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return roundToLongInt(ctx);
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return getNumber().doubleValue();
    }

    @Override
    public Value getAsDateTimeValue(LocaleContext ctx) {
        Value dateValue = DefaultDateFunctions.numberToDateValue(
            getNumber().doubleValue());
        if (dateValue == null) {
            throw invalidConversion(Value.Type.DATE_TIME);
        }
        return dateValue;
    }

    protected abstract Number getNumber();
}
