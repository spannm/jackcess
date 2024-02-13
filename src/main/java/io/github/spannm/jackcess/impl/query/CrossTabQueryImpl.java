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

import io.github.spannm.jackcess.query.CrossTabQuery;

import java.util.List;

/**
 * Concrete Query subclass which represents a crosstab/pivot query, e.g.:
 * {@code TRANSFORM <expr> SELECT <query> PIVOT <expr>}
 *
 * @author James Ahlborn
 */
public class CrossTabQueryImpl extends BaseSelectQueryImpl
    implements CrossTabQuery {

    public CrossTabQueryImpl(String name, List<Row> rows, int objectId,
        int objectFlag) {
        super(name, rows, objectId, objectFlag, Type.CROSS_TAB);
    }

    protected Row getTransformRow() {
        return getUniqueRow(filterRowsByNotFlag(super.getColumnRows(), (short) (CROSSTAB_PIVOT_FLAG | CROSSTAB_NORMAL_FLAG)));
    }

    @Override
    protected List<Row> getColumnRows() {
        return filterRowsByFlag(super.getColumnRows(), CROSSTAB_NORMAL_FLAG);
    }

    @Override
    protected List<Row> getGroupByRows() {
        return filterRowsByFlag(super.getGroupByRows(), CROSSTAB_NORMAL_FLAG);
    }

    protected Row getPivotRow() {
        return getUniqueRow(filterRowsByFlag(super.getColumnRows(),
            CROSSTAB_PIVOT_FLAG));
    }

    @Override
    public String getTransformExpression() {
        Row row = getTransformRow();
        if (row._expression == null) {
            return null;
        }
        // note column expression are always quoted appropriately
        StringBuilder builder = new StringBuilder(row._expression);
        return toAlias(builder, row._name1).toString();
    }

    @Override
    public String getPivotExpression() {
        return getPivotRow()._expression;
    }

    @Override
    protected void toSQLString(StringBuilder builder) {
        String transformExpr = getTransformExpression();
        if (transformExpr != null) {
            builder.append("TRANSFORM ").append(transformExpr).append(NEWLINE);
        }

        toSQLSelectString(builder, true);

        builder.append(NEWLINE).append("PIVOT ")
            .append(getPivotExpression());
    }

}
