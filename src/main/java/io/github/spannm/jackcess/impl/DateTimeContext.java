package io.github.spannm.jackcess.impl;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Provider of zone related info for date/time conversions.
 *
 * @author James Ahlborn
 */
interface DateTimeContext {
    ZoneId getZoneId();

    TimeZone getTimeZone();

    ColumnImpl.DateTimeFactory getDateTimeFactory();
}
