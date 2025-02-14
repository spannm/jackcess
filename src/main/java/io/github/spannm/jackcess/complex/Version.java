package io.github.spannm.jackcess.complex;

import io.github.spannm.jackcess.DateTimeType;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Complex value corresponding to a version of a memo column.
 *
 * @author James Ahlborn
 */
public interface Version extends ComplexValue, Comparable<Version> {
    String getValue();

    /**
     * @deprecated see {@link DateTimeType} for details
     */
    @Deprecated
    Date getModifiedDate();

    LocalDateTime getModifiedLocalDate();

    Object getModifiedDateObject();
}
