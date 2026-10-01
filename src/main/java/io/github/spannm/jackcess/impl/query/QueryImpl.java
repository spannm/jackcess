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
import io.github.spannm.jackcess.RowId;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.RowIdImpl;
import io.github.spannm.jackcess.impl.RowImpl;
import io.github.spannm.jackcess.query.Query;
import io.github.spannm.jackcess.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for classes which encapsulate information about an Access query. The {@link #toSQLString()} method can be
 * used to convert this object into the actual SQL string which this query data represents.
 */
public abstract class QueryImpl implements Query {
    protected static final Logger LOGGER    = Logger.getLogger(QueryImpl.class.getName());

    private static final Row      EMPTY_ROW = new Row();

    private final String          name;
    private final List<Row>       rows;
    private final int             objectId;
    private final int             objectFlag;
    private final Type            type;

    protected QueryImpl(String name, List<Row> rows, int objectId, int objectFlag, Type type) {
        this.name = name;
        this.rows = rows;
        this.objectId = objectId;
        this.type = type;
        this.objectFlag = objectFlag;

        if (type != Type.UNKNOWN) {
            short foundType = getShortValue(getQueryType(rows), type.getValue());
            if (foundType != type.getValue()) {
                throw new IllegalStateException(withErrorContext("Unexpected query type " + foundType));
            }
        }
    }

    /**
     * Returns the name of the query.
     */
    @Override
    public final String getName() {
        return name;
    }

    /**
     * Returns the type of the query.
     */
    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isHidden() {
        return (objectFlag & DatabaseImpl.HIDDEN_OBJECT_FLAG) != 0;
    }

    /**
     * Returns the unique object id of the query.
     */
    @Override
    public int getObjectId() {
        return objectId;
    }

    @Override
    public int getObjectFlag() {
        return objectFlag;
    }

    /**
     * Returns the rows from the system query table from which the query information was derived.
     */
    public List<Row> getRows() {
        return rows;
    }

    protected List<Row> getRowsByAttribute(Byte attribute) {
        return getRowsByAttribute(getRows(), attribute);
    }

    private static List<Row> getRowsByAttribute(List<Row> rows, Byte attribute) {
        List<Row> result = new ArrayList<>();
        for (Row row : rows) {
            if (attribute.equals(row.attribute)) {
                result.add(row);
            }
        }
        return result;
    }

    protected Row getRowByAttribute(Byte attribute) {
        return getUniqueRow(getRowsByAttribute(getRows(), attribute));
    }

    public Row getTypeRow() {
        return getRowByAttribute(QueryFormat.TYPE_ATTRIBUTE);
    }

    protected List<Row> getParameterRows() {
        return getRowsByAttribute(QueryFormat.PARAMETER_ATTRIBUTE);
    }

    protected Row getFlagRow() {
        return getRowByAttribute(QueryFormat.FLAG_ATTRIBUTE);
    }

    protected Row getRemoteDatabaseRow() {
        return getRowByAttribute(QueryFormat.REMOTEDB_ATTRIBUTE);
    }

    protected List<Row> getTableRows() {
        return getRowsByAttribute(QueryFormat.TABLE_ATTRIBUTE);
    }

    protected List<Row> getColumnRows() {
        return getRowsByAttribute(QueryFormat.COLUMN_ATTRIBUTE);
    }

    protected List<Row> getJoinRows() {
        return getRowsByAttribute(QueryFormat.JOIN_ATTRIBUTE);
    }

    protected Row getWhereRow() {
        return getRowByAttribute(QueryFormat.WHERE_ATTRIBUTE);
    }

    protected List<Row> getGroupByRows() {
        return getRowsByAttribute(QueryFormat.GROUPBY_ATTRIBUTE);
    }

    protected Row getHavingRow() {
        return getRowByAttribute(QueryFormat.HAVING_ATTRIBUTE);
    }

    protected List<Row> getOrderByRows() {
        return getRowsByAttribute(QueryFormat.ORDERBY_ATTRIBUTE);
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    protected abstract void toSQLString(StringBuilder builder);

    /**
     * Returns the actual SQL string which this query data represents.
     */
    @Override
    public String toSQLString() {
        StringBuilder builder = new StringBuilder();
        if (supportsStandardClauses()) {
            toSQLParameterString(builder);
        }

        toSQLString(builder);

        if (supportsStandardClauses()) {

            String accessType = getOwnerAccessType();
            if (!QueryFormat.DEFAULT_TYPE.equals(accessType)) {
                builder.append(QueryFormat.NEWLINE).append(accessType);
            }

            builder.append(';');
        }
        return builder.toString();
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    protected void toSQLParameterString(StringBuilder builder) {
        // handle any parameters
        List<String> params = getParameters();
        if (!params.isEmpty()) {
            builder.append("PARAMETERS ").append(params).append(';').append(QueryFormat.NEWLINE);
        }
    }

    @Override
    public List<String> getParameters() {
        return new RowFormatter(getParameterRows()) {
            @Override
            protected void format(StringBuilder builder, Row row) {
                String typeName = DataType.getTypeName(row.flag);
                if (typeName == null) {
                    throw new IllegalStateException(withErrorContext("Unknown param type " + row.flag));
                }

                builder.append(row.name1).append(' ').append(typeName);
                if (QueryFormat.TEXT_FLAG.equals(row.flag) && getIntValue(row.extra, 0) > 0) {
                    builder.append('(').append(row.extra).append(')');
                }
            }
        }.format();
    }

    protected List<String> getFromTables() {
        // grab the list of query tables
        List<TableSource> tableExprs = new ArrayList<>();
        for (Row table : getTableRows()) {
            StringBuilder builder = new StringBuilder();

            if (table.expression != null) {
                toQuotedExpr(builder, table.expression).append(QueryFormat.IDENTIFIER_SEP_CHAR);
            }
            if (table.name1 != null) {
                toOptionalQuotedExpr(builder, table.name1, true);
            }
            toAlias(builder, table.name2);

            String key = table.name2 != null ? table.name2 : table.name1;
            tableExprs.add(new SimpleTable(key, builder.toString()));
        }

        // combine the tables with any query joins
        List<Row> joins = getJoinRows();
        for (Row joinRow : joins) {

            String fromTable = joinRow.name1;
            String toTable = joinRow.name2;

            TableSource fromTs = null;
            TableSource toTs = null;

            // combine existing join expressions containing the target tables
            for (Iterator<TableSource> joinIter = tableExprs.iterator(); joinIter.hasNext() && (fromTs == null || toTs == null);) {
                TableSource ts = joinIter.next();

                if (fromTs == null && ts.containsTable(fromTable)) {
                    fromTs = ts;

                    // special case adding expr to existing join
                    if (toTs == null && ts.containsTable(toTable)) {
                        toTs = ts;
                        break;
                    }

                    joinIter.remove();

                } else if (toTs == null && ts.containsTable(toTable)) {

                    toTs = ts;
                    joinIter.remove();
                }
            }

            if (fromTs == null) {
                fromTs = new SimpleTable(fromTable);
            }
            if (toTs == null) {
                toTs = new SimpleTable(toTable);
            }

            if (fromTs == toTs) { // NOPMD CompareObjectsWithEquals - intentional identity check: did both sides resolve to the same existing TableSource

                if (fromTs.sameJoin(joinRow.flag, joinRow.expression)) {
                    // easy-peasy, we just added the join expression to existing join,
                    // nothing more to do
                    continue;
                }

                throw new IllegalStateException(withErrorContext("Inconsistent join types for " + fromTable + " and " + toTable));
            }

            // new join expression
            tableExprs.add(new Join(fromTs, toTs, joinRow.flag, joinRow.expression));
        }

        // convert join objects to SQL strings
        List<String> result = new AppendableList<>();
        for (TableSource ts : tableExprs) {
            result.add(ts.toString());
        }

        return result;
    }

    protected String getFromRemoteDbPath() {
        return getRemoteDatabaseRow().name1;
    }

    protected String getFromRemoteDbType() {
        return getRemoteDatabaseRow().expression;
    }

    protected String getWhereExpression() {
        return getWhereRow().expression;
    }

    protected List<String> getOrderings() {
        return new RowFormatter(getOrderByRows()) {
            @Override
            protected void format(StringBuilder builder, Row row) {
                builder.append(row.expression);
                if (QueryFormat.DESCENDING_FLAG.equalsIgnoreCase(row.name1)) {
                    builder.append(" DESC");
                }
            }
        }.format();
    }

    @Override
    public String getOwnerAccessType() {
        return hasFlag(QueryFormat.OWNER_ACCESS_SELECT_TYPE) ? "WITH OWNERACCESS OPTION" : QueryFormat.DEFAULT_TYPE;
    }

    protected boolean hasFlag(int flagMask) {
        return hasFlag(getFlagRow(), flagMask);
    }

    protected static boolean hasFlag(Row row, int flagMask) {
        return (getShortValue(row.flag, 0) & flagMask) != 0;
    }

    protected boolean supportsStandardClauses() {
        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.valueBuilder(this).append("name", name).append("rows", rows.size()).append("objectId", objectId).append("type", type).append("objectFlag", objectFlag).toString();
    }

    /**
     * Creates a concrete Query instance from the given query data.
     *
     * @param objectFlag the flag indicating the type of the query
     * @param name the name of the query
     * @param rows the rows from the system query table containing the data describing this query
     * @param objectId the unique object id of this query
     *
     * @return a Query instance for the given query data
     */
    public static QueryImpl create(int objectFlag, String name, List<Row> rows, int objectId) {
        // remove other object flags before testing for query type
        int objTypeFlag = objectFlag & QueryFormat.OBJECT_FLAG_MASK;

        if (objTypeFlag == 0) {
            // sometimes the query rows tell a different story
            short rowTypeFlag = getShortValue(getQueryType(rows), objTypeFlag);
            Type rowType = QueryFormat.TYPE_MAP.get(rowTypeFlag);
            if (rowType != null && rowType.getObjectFlag() != objTypeFlag) {
                // use row type instead of object flag type
                objTypeFlag = rowType.getObjectFlag();
            }
        }

        try {
            switch (objTypeFlag) {
                case QueryFormat.SELECT_QUERY_OBJECT_FLAG:
                    return new SelectQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.MAKE_TABLE_QUERY_OBJECT_FLAG:
                    return new MakeTableQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.APPEND_QUERY_OBJECT_FLAG:
                    return new AppendQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.UPDATE_QUERY_OBJECT_FLAG:
                    return new UpdateQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.DELETE_QUERY_OBJECT_FLAG:
                    return new DeleteQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.CROSS_TAB_QUERY_OBJECT_FLAG:
                    return new CrossTabQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.DATA_DEF_QUERY_OBJECT_FLAG:
                    return new DataDefinitionQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.PASSTHROUGH_QUERY_OBJECT_FLAG:
                    return new PassthroughQueryImpl(name, rows, objectId, objectFlag);
                case QueryFormat.UNION_QUERY_OBJECT_FLAG:
                    return new UnionQueryImpl(name, rows, objectId, objectFlag);
                default:
                    // unknown querytype
                    throw new IllegalStateException(withErrorContext("unknown query object flag " + objTypeFlag, name));
            }
        } catch (IllegalStateException _ex) {
            LOGGER.log(Level.WARNING, "Failed parsing query: " + _ex.getMessage());
        }

        // return unknown query
        return new UnknownQueryImpl(name, rows, objectId, objectFlag);
    }

    private static Short getQueryType(List<Row> rows) {
        return getFirstRowByAttribute(rows, QueryFormat.TYPE_ATTRIBUTE).flag;
    }

    private static Row getFirstRowByAttribute(List<Row> rows, Byte attribute) {
        for (Row row : rows) {
            if (attribute.equals(row.attribute)) {
                return row;
            }
        }
        return EMPTY_ROW;
    }

    protected Row getUniqueRow(List<Row> rowList) {
        if (rowList.size() == 1) {
            return rowList.get(0);
        }
        if (rowList.isEmpty()) {
            return EMPTY_ROW;
        }
        throw new IllegalStateException(withErrorContext("Unexpected number of rows for" + rowList));
    }

    protected static List<Row> filterRowsByFlag(List<Row> rows, final short flag) {
        return new RowFilter() {
            @Override
            protected boolean keep(Row row) {
                return hasFlag(row, flag);
            }
        }.filter(rows);
    }

    protected static List<Row> filterRowsByNotFlag(List<Row> rows, final short flag) {
        return new RowFilter() {
            @Override
            protected boolean keep(Row row) {
                return !hasFlag(row, flag);
            }
        }.filter(rows);
    }

    protected static short getShortValue(Short s, int def) {
        return s != null ? (short) s : (short) def;
    }

    protected static int getIntValue(Integer i, int def) {
        return i != null ? i : def;
    }

    protected static StringBuilder toOptionalQuotedExpr(StringBuilder builder, String fullExpr, boolean isIdentifier) {
        String[] exprs = isIdentifier ? QueryFormat.IDENTIFIER_SEP_PAT.split(fullExpr) : new String[] {fullExpr};
        for (int i = 0; i < exprs.length; ++i) {
            String expr = exprs[i];
            if (QueryFormat.QUOTABLE_CHAR_PAT.matcher(expr).find()) {
                toQuotedExpr(builder, expr);
            } else {
                builder.append(expr);
            }
            if (i < exprs.length - 1) {
                builder.append(QueryFormat.IDENTIFIER_SEP_CHAR);
            }
        }
        return builder;
    }

    protected static StringBuilder toQuotedExpr(StringBuilder builder, String expr) {
        return !isQuoted(expr) ? builder.append('[').append(expr).append(']') : builder.append(expr);
    }

    protected static boolean isQuoted(String expr) {
        return expr.length() >= 2 && expr.charAt(0) == '[' && expr.charAt(expr.length() - 1) == ']';
    }

    protected static StringBuilder toRemoteDb(StringBuilder builder, String remoteDbPath, String remoteDbType) {
        if (remoteDbPath != null || remoteDbType != null) {
            // note, always include path string, even if empty
            builder.append(" IN '");
            if (remoteDbPath != null) {
                builder.append(remoteDbPath);
            }
            builder.append('\'');
            if (remoteDbType != null) {
                builder.append(" [").append(remoteDbType).append(']');
            }
        }
        return builder;
    }

    protected static StringBuilder toAlias(StringBuilder builder, String alias) {
        if (alias != null) {
            toOptionalQuotedExpr(builder.append(" AS "), alias, false);
        }
        return builder;
    }

    private String withErrorContext(String msg) {
        return withErrorContext(msg, getName());
    }

    private static String withErrorContext(String msg, String queryName) {
        return msg + " (Query: " + queryName + ")";
    }

    private static final class UnknownQueryImpl extends QueryImpl {
        private UnknownQueryImpl(String name, List<Row> rows, int objectId, int objectFlag) {
            super(name, rows, objectId, objectFlag, Type.UNKNOWN);
        }

        @Override
        protected void toSQLString(StringBuilder builder) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Struct containing the information from a single row of the system query table.
     */
    public static final class Row {
        private final RowId  id;
        public final Byte    attribute;
        public final String  expression;
        public final Short   flag;
        public final Integer extra;
        public final String  name1;
        public final String  name2;
        public final Integer objectId;
        public final byte[]  order;

        private Row() {
            id = null;
            attribute = null;
            expression = null;
            flag = null;
            extra = null;
            name1 = null;
            name2 = null;
            objectId = null;
            order = null;
        }

        public Row(io.github.spannm.jackcess.Row tableRow) {
            this(tableRow.getId(),
                tableRow.getByte(QueryFormat.COL_ATTRIBUTE),
                tableRow.getString(QueryFormat.COL_EXPRESSION),
                tableRow.getShort(QueryFormat.COL_FLAG),
                tableRow.getInt(QueryFormat.COL_EXTRA),
                tableRow.getString(QueryFormat.COL_NAME1),
                tableRow.getString(QueryFormat.COL_NAME2),
                tableRow.getInt(QueryFormat.COL_OBJECTID),
                tableRow.getBytes(QueryFormat.COL_ORDER));
        }

        public Row(RowId id, Byte attribute, String expression, Short flag, Integer extra, String name1, String name2, Integer objectId, byte[] order) {
            this.id = id;
            this.attribute = attribute;
            this.expression = expression;
            this.flag = flag;
            this.extra = extra;
            this.name1 = name1;
            this.name2 = name2;
            this.objectId = objectId;
            this.order = order;
        }

        public io.github.spannm.jackcess.Row toTableRow() {
            io.github.spannm.jackcess.Row tableRow = new RowImpl((RowIdImpl) id);

            tableRow.put(QueryFormat.COL_ATTRIBUTE, attribute);
            tableRow.put(QueryFormat.COL_EXPRESSION, expression);
            tableRow.put(QueryFormat.COL_FLAG, flag);
            tableRow.put(QueryFormat.COL_EXTRA, extra);
            tableRow.put(QueryFormat.COL_NAME1, name1);
            tableRow.put(QueryFormat.COL_NAME2, name2);
            tableRow.put(QueryFormat.COL_OBJECTID, objectId);
            tableRow.put(QueryFormat.COL_ORDER, order);

            return tableRow;
        }

        @Override
        public String toString() {
            return ToStringBuilder.valueBuilder(this)
                .append("id", id).append("attribute", attribute).append("expression", expression).append("flag", flag).append("extra", extra)
                .append("name1", name1).append("name2", name2).append("objectId", objectId).append("order", order)
                .toString();
        }
    }

    protected abstract static class RowFormatter {
        private final List<Row> list;

        protected RowFormatter(List<Row> list) {
            this.list = list;
        }

        public List<String> format() {
            return format(new AppendableList<>());
        }

        public List<String> format(List<String> strs) {
            for (Row row : list) {
                StringBuilder builder = new StringBuilder();
                format(builder, row);
                strs.add(builder.toString());
            }
            return strs;
        }

        protected abstract void format(StringBuilder builder, Row row);
    }

    protected abstract static class RowFilter {

        public List<Row> filter(List<Row> list) {
            list.removeIf(row -> !keep(row));
            return list;
        }

        protected abstract boolean keep(Row row);
    }

    protected static class AppendableList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 0L;

        protected AppendableList() {
        }

        protected AppendableList(Collection<? extends E> c) {
            super(c);
        }

        protected String getSeparator() {
            return ", ";
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Iterator<E> iter = iterator(); iter.hasNext();) {
                builder.append(iter.next().toString());
                if (iter.hasNext()) {
                    builder.append(getSeparator());
                }
            }
            return builder.toString();
        }
    }

    /**
     * Base type of something which provides table data in a query
     */
    private abstract static class TableSource {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, true);
            return sb.toString();
        }

        @SuppressWarnings("PMD.LinguisticNaming")
        protected abstract void toString(StringBuilder sb, boolean isTopLevel);

        public abstract boolean containsTable(String table);

        public abstract boolean sameJoin(short type, String on);
    }

    /**
     * Table data provided by a single table expression.
     */
    private static final class SimpleTable extends TableSource {
        private final String tableName;
        private final String tableExpr;

        private SimpleTable(String tableName) {
            this(tableName, toOptionalQuotedExpr(new StringBuilder(), tableName, true).toString());
        }

        private SimpleTable(String tableName, String tableExpr) {
            this.tableName = tableName;
            this.tableExpr = tableExpr;
        }

        @Override
        protected void toString(StringBuilder sb, boolean isTopLevel) {
            sb.append(tableExpr);
        }

        @Override
        public boolean containsTable(String table) {
            return tableName.equalsIgnoreCase(table);
        }

        @Override
        public boolean sameJoin(short type, String on) {
            return false;
        }
    }

    /**
     * Table data provided by a join expression.
     */
    private final class Join extends TableSource {
        private final TableSource  from;
        private final TableSource  to;
        private final short        jType;
        // combine all the join expressions with "AND"
        private final List<String> on = new AppendableList<String>() {
                                           private static final long serialVersionUID = 0L;

                                           @Override
                                           protected String getSeparator() {
                                               return ") AND (";
                                           }
                                       };

        private Join(TableSource from, TableSource to, short type, String on) {
            this.from = from;
            this.to = to;
            jType = type;
            this.on.add(on);
        }

        @Override
        protected void toString(StringBuilder sb, boolean isTopLevel) {
            String joinType = QueryFormat.JOIN_TYPE_MAP.get(jType);
            if (joinType == null) {
                throw new IllegalStateException(withErrorContext("Unknown join type " + jType));
            }

            if (!isTopLevel) {
                sb.append('(');
            }

            from.toString(sb, false);
            sb.append(joinType);
            to.toString(sb, false);
            sb.append(" ON ");

            boolean multiOnExpr = on.size() > 1;
            if (multiOnExpr) {
                sb.append('(');
            }
            sb.append(on);
            if (multiOnExpr) {
                sb.append(')');
            }

            if (!isTopLevel) {
                sb.append(')');
            }
        }

        @Override
        public boolean containsTable(String table) {
            return from.containsTable(table) || to.containsTable(table);
        }

        @Override
        public boolean sameJoin(short newType, String onExpr) {
            if (jType == newType) {
                // note, AND conditions are added in _reverse_ order
                on.add(0, onExpr);
                return true;
            }
            return false;
        }
    }
}
