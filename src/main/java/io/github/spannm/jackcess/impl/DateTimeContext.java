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
