/*
Copyright (c) 2016 James Ahlborn

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

import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.RelationshipCreator;
import io.github.spannm.jackcess.impl.RelationshipImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder style class for constructing a {@link Relationship}, and, optionally, the associated backing foreign key (if
 * referential integrity enforcement is enabled). A Relationship can only be constructed for {@link Table}s which
 * already exist in the {@link Database}. Additionally, if integrity enforcement is enabled, there must already be a
 * unique index on the "from" Table for the relevant columns (same requirement as MS Access).
 * <p>
 * Example:
 *
 * <pre>
 * Relationship rel = new RelationshipBuilder("FromTable", "ToTable")
 *     .addColumns("ID", "FK_ID")
 *     .setReferentialIntegrity()
 *     .setCascadeDeletes()
 *     .toRelationship(db);
 * </pre>
 *
 * @author James Ahlborn
 * @see TableBuilder
 */
public class RelationshipBuilder {
    private static final int   JOIN_FLAGS = RelationshipImpl.LEFT_OUTER_JOIN_FLAG | RelationshipImpl.RIGHT_OUTER_JOIN_FLAG;

    /** relationship flags (default to "don't enforce") */
    private int                _flags     = RelationshipImpl.NO_REFERENTIAL_INTEGRITY_FLAG;
    private final String       _fromTable;
    private final String       _toTable;
    private final List<String> _fromCols  = new ArrayList<>();
    private final List<String> _toCols    = new ArrayList<>();
    private String             _name      = null;

    public RelationshipBuilder(Table fromTable, Table toTable) {
        this(fromTable.getName(), toTable.getName());
    }

    public RelationshipBuilder(String fromTable, String toTable) {
        _fromTable = fromTable;
        _toTable = toTable;
    }

    /**
     * Adds a pair of columns to the relationship.
     */
    public RelationshipBuilder addColumns(String fromCol, String toCol) {
        _fromCols.add(fromCol);
        _toCols.add(toCol);
        return this;
    }

    /**
     * Adds a pair of columns to the relationship.
     */
    public RelationshipBuilder addColumns(Column fromCol, Column toCol) {
        return addColumns(fromCol.getName(), toCol.getName());
    }

    /**
     * Enables referential integrity enforcement for this relationship.
     * <p>
     * Note, this requires the "from" table to have an existing unique index on the relevant columns.
     */
    public RelationshipBuilder withReferentialIntegrity() {
        return clearFlag(RelationshipImpl.NO_REFERENTIAL_INTEGRITY_FLAG);
    }

    /**
     * Enables deletes to be cascaded from the "from" table to the "to" table.
     * <p>
     * Note, this requires referential integrity to be enforced.
     */
    public RelationshipBuilder withCascadeDeletes() {
        return withFlag(RelationshipImpl.CASCADE_DELETES_FLAG);
    }

    /**
     * Enables updates to be cascaded from the "from" table to the "to" table.
     * <p>
     * Note, this requires referential integrity to be enforced.
     */
    public RelationshipBuilder withCascadeUpdates() {
        return withFlag(RelationshipImpl.CASCADE_UPDATES_FLAG);
    }

    /**
     * Enables deletes in the "from" table to be cascaded as "null" to the "to" table.
     * <p>
     * Note, this requires referential integrity to be enforced.
     */
    public RelationshipBuilder withCascadeNullOnDelete() {
        return withFlag(RelationshipImpl.CASCADE_NULL_FLAG);
    }

    /**
     * Sets the preferred join type for this relationship.
     */
    public RelationshipBuilder withJoinType(Relationship.JoinType joinType) {
        clearFlag(JOIN_FLAGS);
        switch (joinType) {
            case INNER:
                // nothing to do
                break;
            case LEFT_OUTER:
                _flags |= RelationshipImpl.LEFT_OUTER_JOIN_FLAG;
                break;
            case RIGHT_OUTER:
                _flags |= RelationshipImpl.RIGHT_OUTER_JOIN_FLAG;
                break;
            default:
                throw new RuntimeException("unexpected join type " + joinType);
        }
        return this;
    }

    /**
     * Sets a specific name for this relationship.
     * <p>
     * Default = null, meaning that the standard Access naming convention will be used.
     */
    public RelationshipBuilder withName(String relationshipName) {
        _name = relationshipName;
        return this;
    }

    public boolean hasReferentialIntegrity() {
        return !hasFlag(RelationshipImpl.NO_REFERENTIAL_INTEGRITY_FLAG);
    }

    public int getFlags() {
        return _flags;
    }

    public String getFromTable() {
        return _fromTable;
    }

    public String getToTable() {
        return _toTable;
    }

    public List<String> getFromColumns() {
        return _fromCols;
    }

    public List<String> getToColumns() {
        return _toCols;
    }

    public String getName() {
        return _name;
    }

    /**
     * Creates a new Relationship in the given Database with the currently configured attributes.
     */
    public Relationship toRelationship(Database db) throws IOException {
        return new RelationshipCreator((DatabaseImpl) db).createRelationship(this);
    }

    private RelationshipBuilder withFlag(int flagMask) {
        _flags |= flagMask;
        return this;
    }

    private RelationshipBuilder clearFlag(int flagMask) {
        _flags &= ~flagMask;
        return this;
    }

    private boolean hasFlag(int flagMask) {
        return (_flags & flagMask) != 0;
    }

}
