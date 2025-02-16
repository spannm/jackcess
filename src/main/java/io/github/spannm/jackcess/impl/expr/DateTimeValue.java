package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.impl.ColumnImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DateTimeValue extends BaseValue {
    private final Type          type;
    private final LocalDateTime val;

    public DateTimeValue(Type _type, LocalDateTime _val) {
        if (!_type.isTemporal()) {
            throw new IllegalArgumentException("invalid date/time type");
        }
        type = _type;
        val = _val;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object get() {
        return val;
    }

    protected Double getNumber(LocaleContext ctx) {
        return ColumnImpl.toDateDouble(val);
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        // ms access seems to treat dates/times as "true"
        return true;
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ValueSupport.getDateFormatForType(ctx, getType()).format(val);
    }

    @Override
    public LocalDateTime getAsLocalDateTime(LocaleContext ctx) {
        return val;
    }

    @Override
    public Value getAsDateTimeValue(LocaleContext ctx) {
        return this;
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return roundToLongInt(ctx);
    }

    @Override
    public Double getAsDouble(LocaleContext ctx) {
        return getNumber(ctx);
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(getNumber(ctx));
    }
}
