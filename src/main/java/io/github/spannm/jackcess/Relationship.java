package io.github.spannm.jackcess;

import java.util.List;

/**
 * Information about a relationship between two tables in the {@link Database}.
 *
 * @author James Ahlborn
 */
public interface Relationship {
    enum JoinType {
        INNER,
        LEFT_OUTER,
        RIGHT_OUTER
    }

    String getName();

    Table getFromTable();

    List<Column> getFromColumns();

    Table getToTable();

    List<Column> getToColumns();

    boolean isOneToOne();

    boolean hasReferentialIntegrity();

    boolean cascadeUpdates();

    boolean cascadeDeletes();

    boolean cascadeNullOnDelete();

    boolean isLeftOuterJoin();

    boolean isRightOuterJoin();

    JoinType getJoinType();
}
