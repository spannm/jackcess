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

import static io.github.spannm.jackcess.impl.query.QueryFormat.*;

import io.github.spannm.jackcess.query.BaseSelectQuery;

import java.util.List;

/**
 * Base class for queries which represent some form of SELECT statement.
 */
public abstract class BaseSelectQueryImpl extends QueryImpl implements BaseSelectQuery {

    protected BaseSelectQueryImpl(String name, List<Row> rows, int objectId, int objectFlag, Type type) {
        super(name, rows, objectId, objectFlag, type);
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    protected void toSQLSelectString(StringBuilder builder, boolean useSelectPrefix) {
        if (useSelectPrefix) {
            builder.append("SELECT ");
            String selectType = getSelectType();
            if (!DEFAULT_TYPE.equals(selectType)) {
                builder.append(selectType).append(' ');
            }
        }

        builder.append(getSelectColumns());
        toSelectInto(builder);

        List<String> fromTables = getFromTables();
        if (!fromTables.isEmpty()) {
            builder.append(NEWLINE).append("FROM ").append(fromTables);
            toRemoteDb(builder, getFromRemoteDbPath(), getFromRemoteDbType());
        }

        String whereExpr = getWhereExpression();
        if (whereExpr != null) {
            builder.append(NEWLINE).append("WHERE ").append(whereExpr);
        }

        List<String> groupings = getGroupings();
        if (!groupings.isEmpty()) {
            builder.append(NEWLINE).append("GROUP BY ").append(groupings);
        }

        String havingExpr = getHavingExpression();
        if (havingExpr != null) {
            builder.append(NEWLINE).append("HAVING ").append(havingExpr);
        }

        List<String> orderings = getOrderings();
        if (!orderings.isEmpty()) {
            builder.append(NEWLINE).append("ORDER BY ").append(orderings);
        }
    }

    @Override
    public String getSelectType() {
        if (hasFlag(DISTINCT_SELECT_TYPE)) {
            return "DISTINCT";
        }

        if (hasFlag(DISTINCT_ROW_SELECT_TYPE)) {
            return "DISTINCTROW";
        }

        if (hasFlag(TOP_SELECT_TYPE)) {
            StringBuilder builder = new StringBuilder("TOP ").append(getFlagRow()._name1);
            if (hasFlag(PERCENT_SELECT_TYPE)) {
                builder.append(" PERCENT");
            }
            return builder.toString();
        }

        return DEFAULT_TYPE;
    }

    @Override
    public List<String> getSelectColumns() {
        List<String> result = new RowFormatter(getColumnRows()) {
            @Override
            protected void format(StringBuilder builder, Row row) {
                // note column expression are always quoted appropriately
                builder.append(row._expression);
                toAlias(builder, row._name1);
            }
        }.format();
        if (hasFlag(SELECT_STAR_SELECT_TYPE)) {
            result.add("*");
        }
        return result;
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    protected void toSelectInto(StringBuilder builder) {
        // base does nothing
    }

    @Override
    public List<String> getFromTables() {
        return super.getFromTables();
    }

    @Override
    public String getFromRemoteDbPath() {
        return super.getFromRemoteDbPath();
    }

    @Override
    public String getFromRemoteDbType() {
        return super.getFromRemoteDbType();
    }

    @Override
    public String getWhereExpression() {
        return super.getWhereExpression();
    }

    @Override
    public List<String> getGroupings() {
        return new RowFormatter(getGroupByRows()) {
            @Override
            protected void format(StringBuilder builder, Row row) {
                builder.append(row._expression);
            }
        }.format();
    }

    @Override
    public String getHavingExpression() {
        return getHavingRow()._expression;
    }

    @Override
    public List<String> getOrderings() {
        return super.getOrderings();
    }

}
