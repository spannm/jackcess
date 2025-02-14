package io.github.spannm.jackcess.expr;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * LocaleContext encapsulates all shared localization state for expression parsing and evaluation.
 *
 * @author James Ahlborn
 */
public interface LocaleContext {
    /**
     * @return the currently configured TemporalConfig (from the {@link EvalConfig})
     */
    TemporalConfig getTemporalConfig();

    /**
     * @return an appropriately configured (i.e. locale) DateTimeFormatter for the given format.
     */
    DateTimeFormatter createDateFormatter(String formatStr);

    /**
     * @return the currently configured ZoneId
     */
    ZoneId getZoneId();

    /**
     * @return the currently configured NumericConfig (from the {@link EvalConfig})
     */
    NumericConfig getNumericConfig();

    /**
     * @return an appropriately configured (i.e. DecimalFormatSymbols) DecimalFormat for the given format.
     */
    DecimalFormat createDecimalFormat(String formatStr);
}
