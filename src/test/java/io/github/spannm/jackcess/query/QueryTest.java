/*
Copyright (c) 2007 Health Market Science, Inc.

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

package io.github.spannm.jackcess.query;

import static io.github.spannm.jackcess.impl.query.QueryFormat.*;
import static io.github.spannm.jackcess.test.Basename.QUERY;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.impl.query.QueryImpl;
import io.github.spannm.jackcess.impl.query.QueryImpl.Row;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("checkstyle:LineLengthCheck")
class QueryTest extends AbstractBaseTest {

    @Test
    void testUnionQuery() {
        String expr1 = "Select * from Table1";
        String expr2 = "Select * from Table2";

        UnionQuery query = (UnionQuery) newQuery(
            Query.Type.UNION,
            newRow(TABLE_ATTRIBUTE, expr1, null, UNION_PART1),
            newRow(TABLE_ATTRIBUTE, expr2, null, UNION_PART2));
        setFlag(query, 3);

        assertEquals(multiline("Select * from Table1",
            "UNION Select * from Table2;"),
            query.toSQLString());

        setFlag(query, 1);

        assertEquals(multiline("Select * from Table1",
            "UNION ALL Select * from Table2;"),
            query.toSQLString());

        addRows(query, newRow(ORDERBY_ATTRIBUTE, "Table1.id",
            null, null));

        assertEquals(multiline("Select * from Table1",
            "UNION ALL Select * from Table2",
            "ORDER BY Table1.id;"),
            query.toSQLString());

        removeRows(query, TABLE_ATTRIBUTE);

        assertThrows(IllegalStateException.class, query::toSQLString);
    }

    @Test
    void testPassthroughQuery() {
        String expr = "Select * from Table1";
        String constr = "ODBC;";

        PassthroughQuery query = (PassthroughQuery) newQuery(
            Query.Type.PASSTHROUGH, expr, constr);

        assertEquals(expr, query.toSQLString());
        assertEquals(constr, query.getConnectionString());
    }

    @Test
    void testDataDefinitionQuery() {
        String expr = "Drop table Table1";

        DataDefinitionQuery query = (DataDefinitionQuery) newQuery(
            Query.Type.DATA_DEFINITION, expr, null);

        assertEquals(expr, query.toSQLString());
    }

    @Test
    void testUpdateQuery() {
        UpdateQuery query = (UpdateQuery) newQuery(
            Query.Type.UPDATE,
            newRow(TABLE_ATTRIBUTE, null, "Table1", null),
            newRow(COLUMN_ATTRIBUTE, "\"some string\"", null, "Table1.id"),
            newRow(COLUMN_ATTRIBUTE, "42", null, "Table1.col1"));

        assertEquals(
            multiline("UPDATE Table1",
                "SET Table1.id = \"some string\", Table1.col1 = 42;"),
            query.toSQLString());

        addRows(query, newRow(WHERE_ATTRIBUTE, "(Table1.col2 < 13)",
            null, null));

        assertEquals(
            multiline("UPDATE Table1",
                "SET Table1.id = \"some string\", Table1.col1 = 42",
                "WHERE (Table1.col2 < 13);"),
            query.toSQLString());
    }

    @Test
    void testSelectQuery() {
        SelectQuery query = (SelectQuery) newQuery(
            Query.Type.SELECT,
            newRow(TABLE_ATTRIBUTE, null, "Table1", null));
        setFlag(query, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table1;"),
            query.toSQLString());

        doTestColumns(query);
        doTestSelectFlags(query);
        doTestParameters(query);
        doTestTables(query);
        doTestRemoteDb(query);
        doTestJoins(query);
        doTestWhereExpression(query);
        doTestGroupings(query);
        doTestHavingExpression(query);
        doTestOrderings(query);
    }

    @Test
    void testBadQueries() {
        List<Row> rowList = new ArrayList<>();
        rowList.add(newRow(TYPE_ATTRIBUTE, null, -1, null, null));
        QueryImpl query = QueryImpl.create(-1, "TestQuery", rowList, 13);
        assertThrows(UnsupportedOperationException.class, query::toSQLString);

        addRows(query, newRow(TYPE_ATTRIBUTE, null, -1, null, null));

        assertThrows(IllegalStateException.class, query::getTypeRow);

        assertThrows(IllegalStateException.class, () -> new QueryImpl("TestQuery", rowList, 13, Query.Type.UNION.getObjectFlag(),
            Query.Type.UNION) {
            @Override
            protected void toSQLString(StringBuilder builder) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(QUERY)
    void testReadQueries(TestDb testDb) throws IOException {

        Map<String, String> expectedQueries = new HashMap<>(Map.of(
            "SelectQuery", multiline(
                "SELECT DISTINCT Table1.*, Table2.col1, Table2.col2, Table3.col3",
                "FROM (Table1 LEFT JOIN Table3 ON Table1.col1 = Table3.col1) INNER JOIN Table2 ON (Table3.col1 = Table2.col2) AND (Table3.col1 = Table2.col1)",
                "WHERE (((Table2.col2)=\"foo\" Or (Table2.col2) In (\"buzz\",\"bazz\")))",
                "ORDER BY Table2.col1;"),
            "DeleteQuery", multiline(
                "DELETE Table1.col1, Table1.col2, Table1.col3",
                "FROM Table1",
                "WHERE (((Table1.col1)>\"blah\"));"),
            "AppendQuery", multiline(
                "INSERT INTO Table3 (col2, col2, col3)",
                "SELECT [Table1].[col2], [Table2].[col2], [Table2].[col3]",
                "FROM Table3, Table1 INNER JOIN Table2 ON [Table1].[col1]=[Table2].[col1];"),
            "UpdateQuery", multiline(
                "PARAMETERS User Name Text;",
                "UPDATE Table1",
                "SET Table1.col1 = \"foo\", Table1.col2 = [Table2].[col3], [Table2].[col1] = [User Name]",
                "WHERE ((([Table2].[col1]) Is Not Null));"),
            "MakeTableQuery", multiline(
                "SELECT Max(Table2.col1) AS MaxOfcol1, Table2.col2, Table3.col2 INTO Table4",
                "FROM (Table2 INNER JOIN Table1 ON Table2.col1 = Table1.col2) RIGHT JOIN Table3 ON Table1.col2 = Table3.col3",
                "GROUP BY Table2.col2, Table3.col2",
                "HAVING (((Max(Table2.col1))=\"buzz\") AND ((Table2.col2)<>\"blah\"));"),
            "CrosstabQuery", multiline(
                "TRANSFORM Count([Table2].[col2]) AS CountOfcol2",
                "SELECT Table2_1.col1, [Table2].[col3], Avg(Table2_1.col2) AS AvgOfcol2",
                "FROM (Table1 INNER JOIN Table2 ON [Table1].[col1]=[Table2].[col1]) INNER JOIN Table2 AS Table2_1 ON [Table2].[col1]=Table2_1.col3",
                "WHERE ((([Table1].[col1])>\"10\") And ((Table2_1.col1) Is Not Null) And ((Avg(Table2_1.col2))>\"10\"))",
                "GROUP BY Table2_1.col1, [Table2].[col3]",
                "ORDER BY [Table2].[col3]",
                "PIVOT [Table1].[col1];"),
            "UnionQuery", multiline(
                "Select Table1.col1, Table1.col2",
                "where Table1.col1 = \"foo\"",
                "UNION",
                "Select Table2.col1, Table2.col2",
                "UNION ALL Select Table3.col1, Table3.col2",
                "where Table3.col3 > \"blah\";"),
            "PassthroughQuery", "ALTER TABLE Table4 DROP COLUMN col5;\0",
            "DataDefinitionQuery", "CREATE TABLE Table5 (col1 CHAR, col2 CHAR);\0"));

        try (Database db = testDb.open()) {
            for (Query q : db.getQueries()) {
                assertEquals(expectedQueries.remove(q.getName()), q.toSQLString());
            }

            assertTrue(expectedQueries.isEmpty());
        }
    }

    @Test
    void testAppendQuery() {
        AppendQuery query = (AppendQuery) newQuery(
            Query.Type.APPEND, null, "Table2",
            // newRow(TABLE_ATTRIBUTE, null, "Table1", null),
            newRow(COLUMN_ATTRIBUTE, "54", APPEND_VALUE_FLAG, null, null),
            newRow(COLUMN_ATTRIBUTE, "'hello'", APPEND_VALUE_FLAG, null, null));

        assertEquals(multiline("INSERT INTO Table2",
            "VALUES (54, 'hello');"), query.toSQLString());

        query = (AppendQuery) newQuery(
            Query.Type.APPEND, null, "Table2",
            // newRow(TABLE_ATTRIBUTE, null, "Table1", null),
            newRow(COLUMN_ATTRIBUTE, "54", APPEND_VALUE_FLAG, null, "ID"),
            newRow(COLUMN_ATTRIBUTE, "'hello'", APPEND_VALUE_FLAG, null, "Field 3"));

        assertEquals(multiline("INSERT INTO Table2 (ID, [Field 3])",
            "VALUES (54, 'hello');"), query.toSQLString());
    }

    private void doTestColumns(SelectQuery query) {
        addRows(query, newRow(COLUMN_ATTRIBUTE, "Table1.id", null, null));
        addRows(query, newRow(COLUMN_ATTRIBUTE, "Table1.col", "Some.Alias", null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias], *",
            "FROM Table1;"),
            query.toSQLString());
    }

    private void doTestSelectFlags(SelectQuery query) {
        setFlag(query, 3);

        assertEquals(multiline("SELECT DISTINCT Table1.id, Table1.col AS [Some.Alias], *",
            "FROM Table1;"),
            query.toSQLString());

        setFlag(query, 9);

        assertEquals(multiline("SELECT DISTINCTROW Table1.id, Table1.col AS [Some.Alias], *",
            "FROM Table1;"),
            query.toSQLString());

        setFlag(query, 7);

        assertEquals(multiline("SELECT DISTINCT Table1.id, Table1.col AS [Some.Alias], *",
            "FROM Table1",
            "WITH OWNERACCESS OPTION;"),
            query.toSQLString());

        replaceRows(query,
            newRow(FLAG_ATTRIBUTE, null, 49, null, "5", null));

        assertEquals(multiline("SELECT TOP 5 PERCENT Table1.id, Table1.col AS [Some.Alias], *",
            "FROM Table1;"),
            query.toSQLString());

        setFlag(query, 0);
    }

    private void doTestParameters(SelectQuery query) {
        addRows(query, newRow(PARAMETER_ATTRIBUTE, null, DataType.INT.getValue(), "INT_VAL", null));

        assertEquals(multiline("PARAMETERS INT_VAL Short;",
            "SELECT Table1.id, Table1.col AS [Some.Alias]", "FROM Table1;"),
            query.toSQLString());

        addRows(query, newRow(PARAMETER_ATTRIBUTE, null, DataType.TEXT.getValue(), 50, "TextVal", null),
            newRow(PARAMETER_ATTRIBUTE, null, 0, 50, "[Some Value]", null));

        assertEquals(multiline("PARAMETERS INT_VAL Short, TextVal Text(50), [Some Value] Value;",
            "SELECT Table1.id, Table1.col AS [Some.Alias]", "FROM Table1;"),
            query.toSQLString());

        addRows(query, newRow(PARAMETER_ATTRIBUTE, null, -1, "BadVal", null));

        assertThrows(IllegalStateException.class, query::toSQLString);

        removeRows(query, PARAMETER_ATTRIBUTE);
    }

    private void doTestTables(SelectQuery query) {
        addRows(query, newRow(TABLE_ATTRIBUTE, null, "Table2", "Another Table"));
        addRows(query, newRow(TABLE_ATTRIBUTE, "Select val from Table3", "val", "Table3Val"));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val;"),
            query.toSQLString());
    }

    private void doTestRemoteDb(SelectQuery query) {
        addRows(query, newRow(REMOTEDB_ATTRIBUTE, null, 2, "other_db.mdb", null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val IN 'other_db.mdb';"),
            query.toSQLString());

        replaceRows(query, newRow(REMOTEDB_ATTRIBUTE, "MDB_FILE;", 2, "other_db.mdb", null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val IN 'other_db.mdb' [MDB_FILE;];"),
            query.toSQLString());

        replaceRows(query, newRow(REMOTEDB_ATTRIBUTE, "MDB_FILE;", 2, null, null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val IN '' [MDB_FILE;];"),
            query.toSQLString());

        removeRows(query, REMOTEDB_ATTRIBUTE);
    }

    private void doTestJoins(SelectQuery query) {
        addRows(query, newRow(JOIN_ATTRIBUTE, "(Table1.id = [Another Table].id)", 1, "Table1", "Another Table"));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM [Select val from Table3].val AS Table3Val, Table1 INNER JOIN Table2 AS [Another Table] ON (Table1.id = [Another Table].id);"),
            query.toSQLString());

        addRows(query, newRow(JOIN_ATTRIBUTE, "(Table1.id = Table3Val.id)", 2, "Table1", "Table3Val"));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM (Table1 INNER JOIN Table2 AS [Another Table] ON (Table1.id = [Another Table].id)) LEFT JOIN [Select val from Table3].val AS Table3Val ON (Table1.id = Table3Val.id);"),
            query.toSQLString());

        addRows(query, newRow(JOIN_ATTRIBUTE, "(Table1.id = Table3Val.id)", 5, "Table1", "Table3Val"));

        assertThrows(IllegalStateException.class, query::toSQLString);

        removeLastRows(query, 1);
        query.toSQLString();

        addRows(query, newRow(JOIN_ATTRIBUTE, "(Table1.id = Table3Val.id)", 1, "BogusTable", "Table3Val"));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM BogusTable INNER JOIN ((Table1 INNER JOIN Table2 AS [Another Table] ON (Table1.id = [Another Table].id)) LEFT JOIN [Select val from Table3].val AS Table3Val ON (Table1.id = Table3Val.id)) ON (Table1.id = Table3Val.id);"),
            query.toSQLString());

        removeRows(query, JOIN_ATTRIBUTE);
    }

    private void doTestWhereExpression(SelectQuery query) {
        addRows(query, newRow(WHERE_ATTRIBUTE, "(Table1.col2 < 13)",
            null, null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val",
            "WHERE (Table1.col2 < 13);"),
            query.toSQLString());
    }

    private void doTestGroupings(SelectQuery query) {
        addRows(query, newRow(GROUPBY_ATTRIBUTE, "Table1.id", null, null),
            newRow(GROUPBY_ATTRIBUTE, "SUM(Table1.val)", null, null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val",
            "WHERE (Table1.col2 < 13)",
            "GROUP BY Table1.id, SUM(Table1.val);"),
            query.toSQLString());
    }

    private void doTestHavingExpression(SelectQuery query) {
        addRows(query, newRow(HAVING_ATTRIBUTE, "(SUM(Table1.val) = 500)", null, null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val",
            "WHERE (Table1.col2 < 13)",
            "GROUP BY Table1.id, SUM(Table1.val)",
            "HAVING (SUM(Table1.val) = 500);"),
            query.toSQLString());
    }

    private void doTestOrderings(SelectQuery query) {
        addRows(query, newRow(ORDERBY_ATTRIBUTE, "Table1.id", null, null),
            newRow(ORDERBY_ATTRIBUTE, "Table2.val", "D", null));

        assertEquals(multiline("SELECT Table1.id, Table1.col AS [Some.Alias]",
            "FROM Table1, Table2 AS [Another Table], [Select val from Table3].val AS Table3Val",
            "WHERE (Table1.col2 < 13)",
            "GROUP BY Table1.id, SUM(Table1.val)",
            "HAVING (SUM(Table1.val) = 500)",
            "ORDER BY Table1.id, Table2.val DESC;"),
            query.toSQLString());
    }

    @Test
    void testComplexJoins() {
        SelectQuery query = (SelectQuery) newQuery(
            Query.Type.SELECT);
        setFlag(query, 1);

        for (int i = 1; i <= 10; i++) {
            addRows(query, newRow(TABLE_ATTRIBUTE, null, "Table" + i, null));
        }

        addJoinRows(query, 1, 2, 1, 2, 3, 1, 3, 4, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table5, Table6, Table7, Table8, Table9, Table10, ((Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN Table3 ON Table2.f3 = Table3.f3) INNER JOIN Table4 ON Table3.f6 = Table4.f6;"),
            query.toSQLString());

        addJoinRows(query, 1, 2, 1, 2, 1, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table3, Table4, Table5, Table6, Table7, Table8, Table9, Table10, Table1 INNER JOIN Table2 ON (Table2.f3 = Table1.f3) AND (Table1.f0 = Table2.f0);"),
            query.toSQLString());

        addJoinRows(query, 1, 2, 1, 2, 1, 2);

        assertThrows(IllegalStateException.class, query::toSQLString);

        addJoinRows(query, 1, 2, 1,
            3, 4, 1,
            5, 6, 1,
            7, 8, 1,
            2, 3, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table9, Table10, Table5 INNER JOIN Table6 ON Table5.f6 = Table6.f6, Table7 INNER JOIN Table8 ON Table7.f9 = Table8.f9, (Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN (Table3 INNER JOIN Table4 ON Table3.f3 = Table4.f3) ON Table2.f12 = Table3.f12;"),
            query.toSQLString());

        addJoinRows(query, 1, 2, 1,
            3, 4, 1,
            5, 6, 1,
            7, 8, 1,
            2, 3, 1,
            5, 8, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table9, Table10, (Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN (Table3 INNER JOIN Table4 ON Table3.f3 = Table4.f3) ON Table2.f12 = Table3.f12, (Table5 INNER JOIN Table6 ON Table5.f6 = Table6.f6) INNER JOIN (Table7 INNER JOIN Table8 ON Table7.f9 = Table8.f9) ON Table5.f15 = Table8.f15;"),
            query.toSQLString());

        addJoinRows(query, 1, 2, 1,
            1, 3, 1,
            6, 3, 1,
            1, 9, 2,
            10, 9, 3,
            7, 8, 3,
            1, 8, 2,
            1, 4, 2,
            5, 4, 3);

        assertEquals(multiline("SELECT *",
            "FROM Table5 RIGHT JOIN (((Table10 RIGHT JOIN ((Table6 INNER JOIN ((Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN Table3 ON Table1.f3 = Table3.f3) ON Table6.f6 = Table3.f6) LEFT JOIN Table9 ON Table1.f9 = Table9.f9) ON Table10.f12 = Table9.f12) LEFT JOIN (Table7 RIGHT JOIN Table8 ON Table7.f15 = Table8.f15) ON Table1.f18 = Table8.f18) LEFT JOIN Table4 ON Table1.f21 = Table4.f21) ON Table5.f24 = Table4.f24;"),
            query.toSQLString());

        addJoinRows(query, 1, 2, 1,
            1, 3, 1,
            1, 9, 2,
            1, 8, 2,
            1, 4, 2,
            6, 3, 1,
            5, 4, 3,
            7, 8, 3,
            10, 9, 3);

        assertEquals(multiline("SELECT *",
            "FROM Table10 RIGHT JOIN (Table7 RIGHT JOIN (Table5 RIGHT JOIN (Table6 INNER JOIN (((((Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN Table3 ON Table1.f3 = Table3.f3) LEFT JOIN Table9 ON Table1.f6 = Table9.f6) LEFT JOIN Table8 ON Table1.f9 = Table8.f9) LEFT JOIN Table4 ON Table1.f12 = Table4.f12) ON Table6.f15 = Table3.f15) ON Table5.f18 = Table4.f18) ON Table7.f21 = Table8.f21) ON Table10.f24 = Table9.f24;"),
            query.toSQLString());

        removeRows(query, TABLE_ATTRIBUTE);

        addJoinRows(query, 1, 2, 1,
            2, 3, 1,
            2, 4, 1,
            1, 4, 1,
            5, 3, 1);

        assertEquals(multiline("SELECT *",
            "FROM Table5 INNER JOIN (((Table1 INNER JOIN Table2 ON Table1.f0 = Table2.f0) INNER JOIN Table3 ON Table2.f3 = Table3.f3) INNER JOIN Table4 ON (Table1.f9 = Table4.f9) AND (Table2.f6 = Table4.f6)) ON Table5.f12 = Table3.f12;"),
            query.toSQLString());
    }

    private static void addJoinRows(SelectQuery query, int... joins) {
        removeRows(query, JOIN_ATTRIBUTE);
        for (int i = 0; i < joins.length; i += 3) {
            int from = joins[i + 0];
            int to = joins[i + 1];
            int type = joins[i + 2];

            String tf = "Table" + from;
            String tt = "Table" + to;
            String joinExpr = tf + ".f" + i + " = " + tt + ".f" + i;
            addRows(query, newRow(JOIN_ATTRIBUTE, joinExpr, type, tf, tt));
        }
    }

    private static Query newQuery(Query.Type type, Row... rows) {
        return newQuery(type, null, null, rows);
    }

    private static Query newQuery(Query.Type type, String typeExpr,
        String typeName1, Row... rows) {
        List<Row> rowList = new ArrayList<>();
        rowList.add(newRow(TYPE_ATTRIBUTE, typeExpr, type.getValue(),
            null, typeName1, null));
        rowList.addAll(Arrays.asList(rows));
        return QueryImpl.create(type.getObjectFlag(), "TestQuery", rowList, 13);
    }

    private static Row newRow(Byte attr, String expr, String name1, String name2) {
        return newRow(attr, expr, null, null, name1, name2);
    }

    private static Row newRow(Byte attr, String expr, Number flagNum,
        String name1, String name2) {
        return newRow(attr, expr, flagNum, null, name1, name2);
    }

    private static Row newRow(Byte attr, String expr, Number flagNum,
        Number extraNum, String name1, String name2) {
        Short flag = flagNum != null ? flagNum.shortValue() : null;
        Integer extra = extraNum != null ? extraNum.intValue() : null;
        return new Row(null, attr, expr, flag, extra, name1, name2, null, null);
    }

    private static void setFlag(Query query, Number newFlagNum) {
        replaceRows(query,
            newRow(FLAG_ATTRIBUTE, null, newFlagNum, null, null, null));
    }

    private static void addRows(Query query, Row... rows) {
        ((QueryImpl) query).getRows().addAll(Arrays.asList(rows));
    }

    private static void replaceRows(Query query, Row... rows) {
        removeRows(query, rows[0]._attribute);
        addRows(query, rows);
    }

    private static void removeRows(Query query, Byte attr) {
        ((QueryImpl) query).getRows().removeIf(row -> attr.equals(row._attribute));
    }

    private static void removeLastRows(Query query, int num) {
        List<Row> rows = ((QueryImpl) query).getRows();
        int size = rows.size();
        rows.subList(size - num, size).clear();
    }

    private static String multiline(String... strs) {
        return String.join(System.lineSeparator(), strs);
    }

}
