package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;
import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.impl.ColumnImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DateTimeValue extends BaseValue {
    private final Type          _type;
    private final LocalDateTime _val;

    public DateTimeValue(Type type, LocalDateTime val) {
        if (!type.isTemporal()) {
            throw new IllegalArgumentException("invalid date/time type");
        }
        _type = type;
        _val = val;
    }

    @Override
    public Type getType() {
        return _type;
    }

    @Override
    public Object get() {
        return _val;
    }

    protected Double getNumber(LocaleContext ctx) {
        return ColumnImpl.toDateDouble(_val);
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        // ms access seems to treat dates/times as "true"
        return true;
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return ValueSupport.getDateFormatForType(ctx, getType()).format(_val);
    }

    @Override
    public LocalDateTime getAsLocalDateTime(LocaleContext ctx) {
        return _val;
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
