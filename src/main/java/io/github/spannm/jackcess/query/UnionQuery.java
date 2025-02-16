package io.github.spannm.jackcess.query;

import java.util.List;

/**
 * Query interface which represents a UNION query, e.g.: {@code SELECT <query1> UNION SELECT <query2>}
 */
public interface UnionQuery extends Query {
    String getUnionType();

    String getUnionString1();

    String getUnionString2();

    List<String> getOrderings();
}
