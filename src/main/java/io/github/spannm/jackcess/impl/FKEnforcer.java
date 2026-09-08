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
package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.util.CaseInsensitiveColumnMatcher;
import io.github.spannm.jackcess.util.ColumnMatcher;
import io.github.spannm.jackcess.util.Joiner;

import java.io.IOException;
import java.util.*;

/**
 * Utility class used by Table to enforce foreign-key relationships (if enabled).
 */
final class FKEnforcer {
    // fk constraints always work with indexes, which are always case-insensitive
    private static final ColumnMatcher MATCHER = CaseInsensitiveColumnMatcher.INSTANCE;

    private final TableImpl            table;
    private List<ColumnImpl>           cols;
    private List<Joiner>               primaryJoinersChkUp;
    private List<Joiner>               primaryJoinersChkDel;
    private List<Joiner>               primaryJoinersDoUp;
    private List<Joiner>               primaryJoinersDoDel;
    private List<Joiner>               primaryJoinersDoNull;
    private List<Joiner>               secondaryJoiners;

    FKEnforcer(TableImpl table) {
        this.table = table;

        // at this point, only init the index columns
        initColumns();
    }

    private void initColumns() {
        Set<ColumnImpl> foundCols = new TreeSet<>();
        for (IndexImpl idx : table.getIndexes()) {
            IndexImpl.ForeignKeyReference ref = idx.getReference();
            if (ref != null) {
                // compile an ordered list of all columns in this table which are
                // involved in foreign key relationships with other tables
                for (IndexData.ColumnDescriptor iCol : idx.getColumns()) {
                    foundCols.add(iCol.getColumn());
                }
            }
        }
        cols = !foundCols.isEmpty() ? List.copyOf(foundCols) : List.of();
    }

    /**
     * Resets the internals of this FKEnforcer (for post-table modification)
     */
    void reset() {
        // columns to enforce may have changed
        initColumns();

        // clear any existing joiners (will be re-created on next use)
        primaryJoinersChkUp = null;
        primaryJoinersChkDel = null;
        primaryJoinersDoUp = null;
        primaryJoinersDoDel = null;
        primaryJoinersDoNull = null;
        secondaryJoiners = null;
    }

    /**
     * Does secondary initialization, if necessary.
     */
    private void initialize() throws IOException {
        if (secondaryJoiners != null) {
            // already initialized
            return;
        }

        // initialize all the joiners
        primaryJoinersChkUp = new ArrayList<>(1);
        primaryJoinersChkDel = new ArrayList<>(1);
        primaryJoinersDoUp = new ArrayList<>(1);
        primaryJoinersDoDel = new ArrayList<>(1);
        primaryJoinersDoNull = new ArrayList<>(1);
        secondaryJoiners = new ArrayList<>(1);

        for (IndexImpl idx : table.getIndexes()) {
            IndexImpl.ForeignKeyReference ref = idx.getReference();
            if (ref != null) {

                Joiner joiner = Joiner.create(idx);
                if (ref.isPrimaryTable()) {
                    if (ref.isCascadeUpdates()) {
                        primaryJoinersDoUp.add(joiner);
                    } else {
                        primaryJoinersChkUp.add(joiner);
                    }
                    if (ref.isCascadeDeletes()) {
                        primaryJoinersDoDel.add(joiner);
                    } else if (ref.isCascadeNullOnDelete()) {
                        primaryJoinersDoNull.add(joiner);
                    } else {
                        primaryJoinersChkDel.add(joiner);
                    }
                } else {
                    secondaryJoiners.add(joiner);
                }
            }
        }
    }

    /**
     * Handles foregn-key constraints when adding a row.
     *
     * @param row new row in the Table's row format, including all values used in any foreign-key relationships
     */
    public void addRow(Object[] row) throws IOException {
        if (!enforcing()) {
            return;
        }
        initialize();

        for (Joiner joiner : secondaryJoiners) {
            requirePrimaryValues(joiner, row);
        }
    }

    /**
     * Handles foregn-key constraints when updating a row.
     *
     * @param oldRow old row in the Table's row format, including all values used in any foreign-key relationships
     * @param newRow new row in the Table's row format, including all values used in any foreign-key relationships
     */
    public void updateRow(Object[] oldRow, Object[] newRow) throws IOException {
        if (!enforcing() || !anyUpdates(oldRow, newRow)) {
            // no changes were made to any relevant columns
            return;
        }

        initialize();

        SharedState ss = table.getDatabase().getFKEnforcerSharedState();

        if (ss.isUpdating()) {
            // we only check the primary relationships for the "top-level" of an
            // update operation. in nested levels we are only ever changing the fk
            // values themselves, so we always know the new values are valid.
            for (Joiner joiner : secondaryJoiners) {
                if (anyUpdates(joiner, oldRow, newRow)) {
                    requirePrimaryValues(joiner, newRow);
                }
            }
        }

        ss.pushUpdate();
        try {

            // now, check the tables for which we are the primary table in the
            // relationship (but not cascading)
            for (Joiner joiner : primaryJoinersChkUp) {
                if (anyUpdates(joiner, oldRow, newRow)) {
                    requireNoSecondaryValues(joiner, oldRow);
                }
            }

            // lastly, update the tables for which we are the primary table in the
            // relationship
            for (Joiner joiner : primaryJoinersDoUp) {
                if (anyUpdates(joiner, oldRow, newRow)) {
                    updateSecondaryValues(joiner, oldRow, newRow);
                }
            }

        } finally {
            ss.popUpdate();
        }
    }

    /**
     * Handles foregn-key constraints when deleting a row.
     *
     * @param row old row in the Table's row format, including all values used in any foreign-key relationships
     */
    public void deleteRow(Object[] row) throws IOException {
        if (!enforcing()) {
            return;
        }
        initialize();

        // first, check the tables for which we are the primary table in the
        // relationship (but not cascading)
        for (Joiner joiner : primaryJoinersChkDel) {
            requireNoSecondaryValues(joiner, row);
        }

        // next, delete from the tables for which we are the primary table in
        // the relationship
        for (Joiner joiner : primaryJoinersDoDel) {
            joiner.deleteRows(row);
        }

        // lastly, null the tables for which we are the primary table in
        // the relationship
        for (Joiner joiner : primaryJoinersDoNull) {
            nullSecondaryValues(joiner, row);
        }
    }

    private static void requirePrimaryValues(Joiner joiner, Object[] row) throws IOException {
        // ensure that the relevant rows exist in the primary tables for which
        // this table is a secondary table. however, null values are allowed
        if (!areNull(joiner, row) && !joiner.hasRows(row)) {
            throw new ConstraintViolationException("Adding new row " + Arrays.toString(row) + " violates constraint " + joiner.toFKString());
        }
    }

    private static void requireNoSecondaryValues(Joiner joiner, Object[] row) throws IOException {
        // ensure that no rows exist in the secondary table for which this table is
        // the primary table.
        if (joiner.hasRows(row)) {
            throw new ConstraintViolationException("Removing old row " + Arrays.toString(row) + " violates constraint " + joiner.toFKString());
        }
    }

    private static void updateSecondaryValues(Joiner joiner, Object[] oldFromRow, Object[] newFromRow) throws IOException {
        IndexCursor toCursor = joiner.getToCursor();
        List<? extends Index.Column> fromCols = joiner.getColumns();
        List<? extends Index.Column> toCols = joiner.getToIndex().getColumns();
        Object[] toRow = new Object[joiner.getToTable().getColumnCount()];

        for (Iterator<Row> iter = joiner.findRows(oldFromRow).withColumnNames(Set.of()).iterator(); iter.hasNext();) {
            iter.next();

            // create update row for "to" table
            Arrays.fill(toRow, Column.KEEP_VALUE);
            for (int i = 0; i < fromCols.size(); ++i) {
                Object val = fromCols.get(i).getColumn().getRowValue(newFromRow);
                toCols.get(i).getColumn().setRowValue(toRow, val);
            }

            toCursor.updateCurrentRow(toRow);
        }
    }

    private static void nullSecondaryValues(Joiner joiner, Object[] oldFromRow) throws IOException {
        IndexCursor toCursor = joiner.getToCursor();
        List<? extends Index.Column> fromCols = joiner.getColumns();
        List<? extends Index.Column> toCols = joiner.getToIndex().getColumns();
        Object[] toRow = new Object[joiner.getToTable().getColumnCount()];

        for (Iterator<Row> iter = joiner.findRows(oldFromRow).withColumnNames(Set.of()).iterator(); iter.hasNext();) {
            iter.next();

            // create update row for "to" table
            Arrays.fill(toRow, Column.KEEP_VALUE);
            for (int i = 0; i < fromCols.size(); ++i) {
                toCols.get(i).getColumn().setRowValue(toRow, null);
            }

            toCursor.updateCurrentRow(toRow);
        }
    }

    private boolean anyUpdates(Object[] oldRow, Object[] newRow) {
        for (ColumnImpl col : cols) {
            if (!MATCHER.matches(table, col.getName(), col.getRowValue(oldRow), col.getRowValue(newRow))) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyUpdates(Joiner joiner, Object[] oldRow, Object[] newRow) {
        Table fromTable = joiner.getFromTable();
        for (Index.Column iCol : joiner.getColumns()) {
            Column col = iCol.getColumn();
            if (!MATCHER.matches(fromTable, col.getName(), col.getRowValue(oldRow), col.getRowValue(newRow))) {
                return true;
            }
        }
        return false;
    }

    private static boolean areNull(Joiner joiner, Object[] row) {
        for (Index.Column col : joiner.getColumns()) {
            if (col.getColumn().getRowValue(row) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean enforcing() {
        return table.getDatabase().isEnforceForeignKeys();
    }

    static SharedState initSharedState() {
        return new SharedState();
    }

    /**
     * Shared state used by all FKEnforcers for a given Database.
     */
    static final class SharedState {
        /** current depth of cascading update calls across one or more tables */
        private int updateDepth;

        private SharedState() {
        }

        public boolean isUpdating() {
            return updateDepth == 0;
        }

        public void pushUpdate() {
            ++updateDepth;
        }

        public void popUpdate() {
            --updateDepth;
        }
    }
}
