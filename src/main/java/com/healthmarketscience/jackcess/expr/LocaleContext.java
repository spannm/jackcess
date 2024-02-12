/*
Copyright (c) 2018 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess.expr;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * LocaleContext encapsulates all shared localization state for expression
 * parsing and evaluation.
 *
 * @author James Ahlborn
 */
public interface LocaleContext
{
  /**
   * @return the currently configured TemporalConfig (from the
   *         {@link EvalConfig})
   */
  public TemporalConfig getTemporalConfig();

  /**
   * @return an appropriately configured (i.e. locale) DateTimeFormatter for
   *         the given format.
   */
  public DateTimeFormatter createDateFormatter(String formatStr);

  /**
   * @return the currently configured ZoneId
   */
  public ZoneId getZoneId();

  /**
   * @return the currently configured NumericConfig (from the
   *         {@link EvalConfig})
   */
  public NumericConfig getNumericConfig();

  /**
   * @return an appropriately configured (i.e. DecimalFormatSymbols)
   *         DecimalFormat for the given format.
   */
  public DecimalFormat createDecimalFormat(String formatStr);
}
