/*
Copyright (c) 2008 Health Market Science, Inc.

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

package io.github.spannm.jackcess.impl.query;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.query.Query;
import io.github.spannm.jackcess.query.Query.Type;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Constants used by the query data parsing.
 *
 * @author James Ahlborn
 */
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public class QueryFormat {

    private QueryFormat() {
    }

    public static final int                SELECT_QUERY_OBJECT_FLAG      = 0;
    public static final int                MAKE_TABLE_QUERY_OBJECT_FLAG  = 80;
    public static final int                APPEND_QUERY_OBJECT_FLAG      = 64;
    public static final int                UPDATE_QUERY_OBJECT_FLAG      = 48;
    public static final int                DELETE_QUERY_OBJECT_FLAG      = 32;
    public static final int                CROSS_TAB_QUERY_OBJECT_FLAG   = 16;
    public static final int                DATA_DEF_QUERY_OBJECT_FLAG    = 96;
    public static final int                PASSTHROUGH_QUERY_OBJECT_FLAG = 112;
    public static final int                UNION_QUERY_OBJECT_FLAG       = 128;
    // dbQSPTBulk = 144
    // dbQCompound = 160
    // dbQProcedure = 224
    // dbQAction = 240

    // mask which removes superfluous flags from object flags
    static final int                       OBJECT_FLAG_MASK              = 0xF0;

    public static final String             COL_ATTRIBUTE                 = "Attribute";
    public static final String             COL_EXPRESSION                = "Expression";
    public static final String             COL_FLAG                      = "Flag";
    public static final String             COL_EXTRA                     = "LvExtra";
    public static final String             COL_NAME1                     = "Name1";
    public static final String             COL_NAME2                     = "Name2";
    public static final String             COL_OBJECTID                  = "ObjectId";
    public static final String             COL_ORDER                     = "Order";

    public static final Byte               START_ATTRIBUTE               = 0;
    public static final Byte               TYPE_ATTRIBUTE                = 1;
    public static final Byte               PARAMETER_ATTRIBUTE           = 2;
    public static final Byte               FLAG_ATTRIBUTE                = 3;
    public static final Byte               REMOTEDB_ATTRIBUTE            = 4;
    public static final Byte               TABLE_ATTRIBUTE               = 5;
    public static final Byte               COLUMN_ATTRIBUTE              = 6;
    public static final Byte               JOIN_ATTRIBUTE                = 7;
    public static final Byte               WHERE_ATTRIBUTE               = 8;
    public static final Byte               GROUPBY_ATTRIBUTE             = 9;
    public static final Byte               HAVING_ATTRIBUTE              = 10;
    public static final Byte               ORDERBY_ATTRIBUTE             = 11;
    public static final Byte               END_ATTRIBUTE                 = (byte) 255;

    public static final short              UNION_FLAG                    = 0x02;

    public static final Short              TEXT_FLAG                     = (short) DataType.TEXT.getValue();

    public static final String             DESCENDING_FLAG               = "D";

    public static final short              SELECT_STAR_SELECT_TYPE       = 0x01;
    public static final short              DISTINCT_SELECT_TYPE          = 0x02;
    public static final short              OWNER_ACCESS_SELECT_TYPE      = 0x04;
    public static final short              DISTINCT_ROW_SELECT_TYPE      = 0x08;
    public static final short              TOP_SELECT_TYPE               = 0x10;
    public static final short              PERCENT_SELECT_TYPE           = 0x20;

    public static final short              APPEND_VALUE_FLAG             = (short) 0x8000;

    public static final short              CROSSTAB_PIVOT_FLAG           = 0x01;
    public static final short              CROSSTAB_NORMAL_FLAG          = 0x02;

    public static final String             UNION_PART1                   = "X7YZ_____1";
    public static final String             UNION_PART2                   = "X7YZ_____2";

    public static final String             DEFAULT_TYPE                  = "";

    public static final Pattern            QUOTABLE_CHAR_PAT             = Pattern.compile("\\W");

    public static final Pattern            IDENTIFIER_SEP_PAT            = Pattern.compile("\\.");
    public static final char               IDENTIFIER_SEP_CHAR           = '.';

    public static final String             NEWLINE                       = System.lineSeparator();

    public static final Map<Short, String> JOIN_TYPE_MAP = Map.of(
        (short) 1, " INNER JOIN ",
        (short) 2, " LEFT JOIN ",
        (short) 3, " RIGHT JOIN ");

    public static final Map<Short, Query.Type> TYPE_MAP      = Arrays.stream(Query.Type.values())
        .filter(Type::isUnknown).collect(Collectors.toMap(Type::getValue, v -> v));

}
