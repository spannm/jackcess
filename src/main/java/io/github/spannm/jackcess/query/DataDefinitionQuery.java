package io.github.spannm.jackcess.query;

/**
 * Query interface which represents a DDL query.
 *
 * @author James Ahlborn
 */
public interface DataDefinitionQuery extends Query {
    String getDDLString();
}
