package io.github.spannm.jackcess.query;

import java.util.List;

/**
 * Base interface for queries which represent some form of SELECT statement.
 */
public interface BaseSelectQuery extends Query {

    String getSelectType();

    List<String> getSelectColumns();

    List<String> getFromTables();

    String getFromRemoteDbPath();

    String getFromRemoteDbType();

    String getWhereExpression();

    List<String> getGroupings();

    String getHavingExpression();

    List<String> getOrderings();
}
