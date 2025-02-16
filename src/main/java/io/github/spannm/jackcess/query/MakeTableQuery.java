package io.github.spannm.jackcess.query;

/**
 * Query interface which represents an table creation query, e.g.: {@code SELECT <query> INTO <newTable>}
 */
public interface MakeTableQuery extends BaseSelectQuery {

    String getTargetTable();

    String getRemoteDbPath();

    String getRemoteDbType();
}
