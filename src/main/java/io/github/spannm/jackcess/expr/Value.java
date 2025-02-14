package io.github.spannm.jackcess.expr;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wrapper for a typed primitive value used within the expression evaluation engine. Note that the "Null" value is
 * represented by an actual Value instance with the type of {@link Type#NULL}. Also note that all the conversion methods
 * will throw an {@link EvalException} if the conversion is not supported for the current value.
 *
 * @author James Ahlborn
 */
public interface Value {
    /** the types supported within the expression evaluation engine */
    enum Type {
        NULL,
        STRING,
        DATE,
        TIME,
        DATE_TIME,
        LONG,
        DOUBLE,
        BIG_DEC;

        public boolean isString() {
            return this == STRING;
        }

        public boolean isNumeric() {
            return inRange(LONG, BIG_DEC);
        }

        public boolean isIntegral() {
            return this == LONG;
        }

        public boolean isTemporal() {
            return inRange(DATE, DATE_TIME);
        }

        public Type getPreferredFPType() {
            return ordinal() <= DOUBLE.ordinal() ? DOUBLE : BIG_DEC;
        }

        public Type getPreferredNumericType() {
            if (isNumeric()) {
                return this;
            }
            if (isTemporal()) {
                return this == DATE ? LONG : DOUBLE;
            }
            return null;
        }

        private boolean inRange(Type start, Type end) {
            return start.ordinal() <= ordinal() && ordinal() <= end.ordinal();
        }
    }

    /**
     * @return the type of this value
     */
    Type getType();

    /**
     * @return the raw primitive value
     */
    Object get();

    /**
     * @return {@code true} if this value represents a "Null" value, {@code false} otherwise.
     */
    boolean isNull();

    /**
     * @return this primitive value converted to a boolean
     */
    boolean getAsBoolean(LocaleContext ctx);

    /**
     * @return this primitive value converted to a String
     */
    String getAsString(LocaleContext ctx);

    /**
     * @return this primitive value converted to a LocalDateTime
     */
    LocalDateTime getAsLocalDateTime(LocaleContext ctx);

    /**
     * Since date/time values have different types, it may be more convenient to get the date/time primitive value with
     * the appropriate type information.
     *
     * @return this value converted to a date/time value
     */
    Value getAsDateTimeValue(LocaleContext ctx);

    /**
     * @return this primitive value converted (rounded) to an int
     */
    Integer getAsLongInt(LocaleContext ctx);

    /**
     * @return this primitive value converted (rounded) to a double
     */
    Double getAsDouble(LocaleContext ctx);

    /**
     * @return this primitive value converted to a BigDecimal
     */
    BigDecimal getAsBigDecimal(LocaleContext ctx);
}
