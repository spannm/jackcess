package io.github.spannm.jackcess.query;

import java.util.List;

/**
 * Query interface which represents a row update query,
 * e.g.: {@code UPDATE table SET newValues}
 *
 * @author James Ahlborn
 */
public interface UpdateQuery extends Query {

    List<String> getTargetTables();

    String getRemoteDbPath();

    String getRemoteDbType();

    List<String> getNewValues();

    String getWhereExpression();
}
