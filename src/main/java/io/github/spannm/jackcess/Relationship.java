/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess;

import java.util.List;

/**
 * Information about a relationship between two tables in the {@link Database}.
 */
public interface Relationship {
    /**
     * The type of join used when this relationship is followed, mirroring Access's own join type options.
     */
    enum JoinType {
        INNER,
        LEFT_OUTER,
        RIGHT_OUTER
    }

    /**
     * Returns the name of this relationship.
     *
     * @return the name of this relationship
     */
    String getName();

    /**
     * Returns the "one" (or referenced) side table of this relationship.
     *
     * @return the "one" (or referenced) side table of this relationship
     */
    Table getFromTable();

    /**
     * Returns the columns of {@link #getFromTable} which participate in this relationship, in the same order as the
     * corresponding columns returned by {@link #getToColumns}.
     *
     * @return the columns of {@link #getFromTable} which participate in this relationship, in the same order as the
     *         corresponding columns returned by {@link #getToColumns}
     */
    List<Column> getFromColumns();

    /**
     * Returns the "many" (or referencing) side table of this relationship.
     *
     * @return the "many" (or referencing) side table of this relationship
     */
    Table getToTable();

    /**
     * Returns the columns of {@link #getToTable} which participate in this relationship, in the same order as the
     * corresponding columns returned by {@link #getFromColumns}.
     *
     * @return the columns of {@link #getToTable} which participate in this relationship, in the same order as the
     *         corresponding columns returned by {@link #getFromColumns}
     */
    List<Column> getToColumns();

    /**
     * Returns {@code true} if this relationship enforces a one-to-one association, {@code false} if one-to-many.
     *
     * @return {@code true} if this relationship enforces a one-to-one association, {@code false} if one-to-many
     */
    boolean isOneToOne();

    /**
     * Returns {@code true} if this relationship enforces referential integrity.
     *
     * @return {@code true} if this relationship enforces referential integrity
     */
    boolean hasReferentialIntegrity();

    /**
     * Returns {@code true} if updates to the "from" table's key are cascaded to the "to" table (only meaningful if
     * {@link #hasReferentialIntegrity} is {@code true}).
     *
     * @return {@code true} if updates to the "from" table's key are cascaded to the "to" table (only meaningful if
     *         {@link #hasReferentialIntegrity} is {@code true})
     */
    boolean cascadeUpdates();

    /**
     * Returns {@code true} if deletes of "from" table rows cascade to delete the corresponding "to" table rows (only
     * meaningful if {@link #hasReferentialIntegrity} is {@code true}).
     *
     * @return {@code true} if deletes of "from" table rows cascade to delete the corresponding "to" table rows (only
     *         meaningful if {@link #hasReferentialIntegrity} is {@code true})
     */
    boolean cascadeDeletes();

    /**
     * Returns {@code true} if deletes of "from" table rows null out the corresponding foreign key in the "to" table
     * rows, rather than deleting them (only meaningful if {@link #hasReferentialIntegrity} is {@code true}).
     *
     * @return {@code true} if deletes of "from" table rows null out the corresponding foreign key in the "to" table
     *         rows, rather than deleting them (only meaningful if {@link #hasReferentialIntegrity} is {@code true})
     */
    boolean cascadeNullOnDelete();

    /**
     * Returns {@code true} if this relationship is enforced as a left outer join.
     *
     * @return {@code true} if this relationship is enforced as a left outer join
     */
    boolean isLeftOuterJoin();

    /**
     * Returns {@code true} if this relationship is enforced as a right outer join.
     *
     * @return {@code true} if this relationship is enforced as a right outer join
     */
    boolean isRightOuterJoin();

    /**
     * Returns the join type used when this relationship is followed.
     *
     * @return the join type used when this relationship is followed
     */
    JoinType getJoinType();
}
