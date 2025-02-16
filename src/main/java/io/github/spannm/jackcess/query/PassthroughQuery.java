package io.github.spannm.jackcess.query;

/**
 * Query interface which represents a query which will be executed via ODBC.
 */
public interface PassthroughQuery extends Query {
    String getConnectionString();

    String getPassthroughString();
}
