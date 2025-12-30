/*
 * Copyright (C) 2024- Markus Spann
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
package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.util.StringUtil;

/**
 * Defines known valid database test file base names
 * i.e. the prefix of the file name.
 */
public enum Basename {

    BIG_INDEX,
    BINARY_INDEX,
    BLOB,
    CALC_FIELD,
    COMMON1,
    COMMON2,
    COMPLEX_DATA,
    COMP_INDEX,
    DEL,
    DEL_COL,
    EMOTICONS,
    EXT_DATE,
    FIXED_NUMERIC,
    FIXED_TEXT,
    INDEX,
    INDEX_CODES,
    INDEX_CURSOR,
    INDEX_PROPERTIES,
    LINKED,
    LINKED_ODBC,
    OLD_DATES,
    OVERFLOW,
    PROMOTION,
    QUERY,
    REF_GLOBAL,
    UNICODE_COMP,
    UNSUPPORTED_FIELDS;

    private final String basename;

    Basename() {
        basename = StringUtil.toTitleCase(name());
    }

    @Override
    public String toString() {
        return basename;
    }

}
