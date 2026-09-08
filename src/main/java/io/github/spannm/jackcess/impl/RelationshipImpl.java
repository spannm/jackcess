/*
Copyright (c) 2008 Health Market Science, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Relationship;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Information about a relationship between two tables in the database.
 */
public class RelationshipImpl implements Relationship {

    /** flag indicating one-to-one relationship */
    public static final int    ONE_TO_ONE_FLAG               = 0x00000001;
    /** flag indicating no referential integrity */
    public static final int    NO_REFERENTIAL_INTEGRITY_FLAG = 0x00000002;
    /** flag indicating cascading updates (requires referential integrity) */
    public static final int    CASCADE_UPDATES_FLAG          = 0x00000100;
    /** flag indicating cascading deletes (requires referential integrity) */
    public static final int    CASCADE_DELETES_FLAG          = 0x00001000;
    /**
     * flag indicating cascading null on delete (requires referential integrity)
     */
    public static final int    CASCADE_NULL_FLAG             = 0x00002000;
    /** flag indicating left outer join */
    public static final int    LEFT_OUTER_JOIN_FLAG          = 0x01000000;
    /** flag indicating right outer join */
    public static final int    RIGHT_OUTER_JOIN_FLAG         = 0x02000000;

    /** the name of this relationship */
    private final String       name;
    /** the "from" table in this relationship */
    private final Table        fromTable;
    /** the "to" table in this relationship */
    private final Table        toTable;
    /**
     * the columns in the "from" table in this relationship (aligned w/ toColumns list)
     */
    private final List<Column> toColumns;
    /**
     * the columns in the "to" table in this relationship (aligned w/ toColumns list)
     */
    private final List<Column> fromColumns;
    /** the various flags describing this relationship */
    private final int          flags;

    public RelationshipImpl(String name, Table fromTable, Table toTable, int flags, int numCols) {
        this(name, fromTable, toTable, flags, Collections.nCopies(numCols, null), Collections.nCopies(numCols, null));
    }

    public RelationshipImpl(String name, Table fromTable, Table toTable, int flags, List<? extends Column> fromCols, List<? extends Column> toCols) {
        this.name = name;
        this.fromTable = fromTable;
        fromColumns = new ArrayList<>(fromCols);
        this.toTable = toTable;
        toColumns = new ArrayList<>(toCols);
        this.flags = flags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Table getFromTable() {
        return fromTable;
    }

    @Override
    public List<Column> getFromColumns() {
        return fromColumns;
    }

    @Override
    public Table getToTable() {
        return toTable;
    }

    @Override
    public List<Column> getToColumns() {
        return toColumns;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public boolean isOneToOne() {
        return hasFlag(ONE_TO_ONE_FLAG);
    }

    @Override
    public boolean hasReferentialIntegrity() {
        return !hasFlag(NO_REFERENTIAL_INTEGRITY_FLAG);
    }

    @Override
    public boolean cascadeUpdates() {
        return hasFlag(CASCADE_UPDATES_FLAG);
    }

    @Override
    public boolean cascadeDeletes() {
        return hasFlag(CASCADE_DELETES_FLAG);
    }

    @Override
    public boolean cascadeNullOnDelete() {
        return hasFlag(CASCADE_NULL_FLAG);
    }

    @Override
    public boolean isLeftOuterJoin() {
        return hasFlag(LEFT_OUTER_JOIN_FLAG);
    }

    @Override
    public boolean isRightOuterJoin() {
        return hasFlag(RIGHT_OUTER_JOIN_FLAG);
    }

    @Override
    public JoinType getJoinType() {
        if (isLeftOuterJoin()) {
            return JoinType.LEFT_OUTER;
        } else if (isRightOuterJoin()) {
            return JoinType.RIGHT_OUTER;
        }
        return JoinType.INNER;
    }

    private boolean hasFlag(int flagMask) {
        return (getFlags() & flagMask) != 0;
    }

    @Override
    public String toString() {
        return ToStringBuilder.builder(this)
            .append("name", name)
            .append("fromTable", fromTable.getName())
            .append("fromColumns", fromColumns)
            .append("toTable", toTable.getName())
            .append("toColumns", toColumns)
            .append("flags", Integer.toHexString(flags))
            .toString();
    }

}
