package io.github.spannm.jackcess;

/**
 * Enum for selecting how a Database returns date/time types. Prefer using {@link DateTimeType#LOCAL_DATE_TIME} as using
 * Date is being phased out and will eventually be removed.
 */
public enum DateTimeType {
    /**
     * Use legacy {@link java.util.Date} objects. This was the default for Jackcess before version 3.5.
     */
    DATE,
    /**
     * Use jdk8+ {@link java.time.LocalDateTime} objects. This is the default for Jackcess from version 3.5 onwards.
     */
    LOCAL_DATE_TIME
}
