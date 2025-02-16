package io.github.spannm.jackcess.query;

/**
 * Query interface which represents a crosstab/pivot query, e.g.: {@code TRANSFORM <expr> SELECT <query> PIVOT <expr>}
 */
public interface CrossTabQuery extends BaseSelectQuery {

    String getTransformExpression();

    String getPivotExpression();
}
