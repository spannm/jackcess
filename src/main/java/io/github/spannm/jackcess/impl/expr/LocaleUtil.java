/*
 * Copyright (c) 2026 James Ahlborn
 * Copyright (c) 2026 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.impl.expr;

import java.util.Locale;
import java.util.Map;

/**
 * Maps Windows Locale Identifier (LCID) values, as used for MS Access collating sort orders, to a display name and a
 * corresponding Java {@link Locale}.
 * <p>
 * Contains only the LCIDs that are known to appear as database-level or column-level sort orders in MS Access
 * databases.
 */
public final class LocaleUtil {

    private static final Map<Integer, LcidInfo> LCID_TO_INFO = Map.ofEntries(
        // General / English
        entry(1033, "General", Locale.US),
        // Western European languages
        entry(1031, "German", Locale.GERMAN),
        entry(1036, "French", Locale.FRENCH),
        entry(1034, "Spanish", lc("es")),
        entry(1040, "Italian", Locale.ITALIAN),
        entry(1043, "Dutch", lc("nl")),
        entry(1046, "Portuguese", lc("pt")),
        entry(1053, "Swedish", lc("sv")),
        entry(1030, "Danish", lc("da")),
        entry(1044, "Norwegian", lc("no")),
        entry(1035, "Finnish", lc("fi")),
        // Central/Eastern European
        entry(1045, "Polish", lc("pl")),
        entry(1029, "Czech", lc("cs")),
        entry(1038, "Hungarian", lc("hu")),
        entry(1050, "Croatian", lc("hr")),
        entry(1051, "Slovak", lc("sk")),
        entry(1060, "Slovenian", lc("sl")),
        entry(1048, "Romanian", lc("ro")),
        entry(1026, "Bulgarian", lc("bg")),
        // Cyrillic
        entry(1049, "Russian", lc("ru")),
        entry(1058, "Ukrainian", lc("uk")),
        // Baltic
        entry(1061, "Estonian", lc("et")),
        entry(1062, "Latvian", lc("lv")),
        entry(1063, "Lithuanian", lc("lt")),
        // Turkish and related
        entry(1055, "Turkish", lc("tr")),
        entry(1068, "Azerbaijani", lc("az")),
        // Greek
        entry(1032, "Greek", lc("el")),
        // East Asian
        entry(1041, "Japanese", Locale.JAPANESE),
        entry(1042, "Korean", Locale.KOREAN),
        entry(2052, "Chinese Simplified", Locale.SIMPLIFIED_CHINESE),
        entry(1028, "Chinese Traditional", Locale.TRADITIONAL_CHINESE),
        // Arabic / Hebrew
        entry(1025, "Arabic", lc("ar")),
        entry(1037, "Hebrew", lc("he")),
        // Nordic/Romance
        entry(1069, "Basque", lc("eu")),
        entry(1027, "Catalan", lc("ca")));

    private LocaleUtil() {
    }

    /**
     * Returns the display name and {@link Locale} for the given Windows LCID, or {@code null} if the LCID is not
     * recognised.
     */
    public static LcidInfo getInfo(int localeId) {
        return LCID_TO_INFO.get(localeId);
    }

    private static Locale lc(String languageTag) {
        return Locale.forLanguageTag(languageTag);
    }

    private static Map.Entry<Integer, LcidInfo> entry(int lcid, String name, Locale locale) {
        return Map.entry(lcid, new LcidInfo(name, locale));
    }

    /**
     * Holds the display name and Java {@link Locale} for a Windows LCID.
     */
    public static final class LcidInfo {
        private final String name;
        private final Locale locale;

        private LcidInfo(String name, Locale locale) {
            this.name = name;
            this.locale = locale;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
