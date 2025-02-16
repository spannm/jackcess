package io.github.spannm.jackcess.query;

import java.util.List;

/**
 * Query interface which represents an append query,
 * e.g.: {@code INSERT INTO table VALUES (values)}
 */
public interface AppendQuery extends BaseSelectQuery {

    String getTargetTable();

    List<String> getTargetColumns();

    String getRemoteDbPath();

    String getRemoteDbType();

    List<String> getValues();
}
