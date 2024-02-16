/*
Copyright (c) 2013 James Ahlborn

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
