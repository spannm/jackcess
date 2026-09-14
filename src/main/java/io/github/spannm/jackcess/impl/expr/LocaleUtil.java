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

import java.util.Collections;
import java.util.HashMap;
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

    private static final Map<Integer, LcidInfo> LCID_TO_INFO;
    static {
        Map<Integer, LcidInfo> m = new HashMap<>();
        // General / English
        entry(m, 1033, "General", Locale.US);
        // Western European languages
        entry(m, 1031, "German", Locale.GERMAN);
        entry(m, 1036, "French", Locale.FRENCH);
        entry(m, 1034, "Spanish", lc("es"));
        entry(m, 1040, "Italian", Locale.ITALIAN);
        entry(m, 1043, "Dutch", lc("nl"));
        entry(m, 1046, "Portuguese", lc("pt"));
        entry(m, 1053, "Swedish", lc("sv"));
        entry(m, 1030, "Danish", lc("da"));
        entry(m, 1044, "Norwegian", lc("no"));
        entry(m, 1035, "Finnish", lc("fi"));
        // Central/Eastern European
        entry(m, 1045, "Polish", lc("pl"));
        entry(m, 1029, "Czech", lc("cs"));
        entry(m, 1038, "Hungarian", lc("hu"));
        entry(m, 1050, "Croatian", lc("hr"));
        entry(m, 1051, "Slovak", lc("sk"));
        entry(m, 1060, "Slovenian", lc("sl"));
        entry(m, 1048, "Romanian", lc("ro"));
        entry(m, 1026, "Bulgarian", lc("bg"));
        // Cyrillic
        entry(m, 1049, "Russian", lc("ru"));
        entry(m, 1058, "Ukrainian", lc("uk"));
        // Baltic
        entry(m, 1061, "Estonian", lc("et"));
        entry(m, 1062, "Latvian", lc("lv"));
        entry(m, 1063, "Lithuanian", lc("lt"));
        // Turkish and related
        entry(m, 1055, "Turkish", lc("tr"));
        entry(m, 1068, "Azerbaijani", lc("az"));
        // Greek
        entry(m, 1032, "Greek", lc("el"));
        // East Asian
        entry(m, 1041, "Japanese", Locale.JAPANESE);
        entry(m, 1042, "Korean", Locale.KOREAN);
        entry(m, 2052, "Chinese Simplified", Locale.SIMPLIFIED_CHINESE);
        entry(m, 1028, "Chinese Traditional", Locale.TRADITIONAL_CHINESE);
        // Arabic / Hebrew
        entry(m, 1025, "Arabic", lc("ar"));
        entry(m, 1037, "Hebrew", lc("he"));
        // Nordic/Romance
        entry(m, 1069, "Basque", lc("eu"));
        entry(m, 1027, "Catalan", lc("ca"));
        LCID_TO_INFO = Collections.unmodifiableMap(m);
    }

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

    private static void entry(Map<Integer, LcidInfo> m, int lcid, String name, Locale locale) {
        m.put(lcid, new LcidInfo(name, locale));
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
