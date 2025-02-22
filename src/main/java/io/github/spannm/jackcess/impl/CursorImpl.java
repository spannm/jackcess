/*
Copyright (c) 2007 Health Market Science, Inc.

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

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.impl.TableImpl.RowState;
import io.github.spannm.jackcess.util.ColumnMatcher;
import io.github.spannm.jackcess.util.ErrorHandler;
import io.github.spannm.jackcess.util.IterableBuilder;
import io.github.spannm.jackcess.util.SimpleColumnMatcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.*;
import java.util.function.Predicate;

/**
 * Manages iteration for a Table. Different cursors provide different methods of traversing a table. Cursors should be
 * fairly robust in the face of table modification during traversal (although depending on how the table is traversed,
 * row updates may or may not be seen). Multiple cursors may traverse the same table simultaneously.
 * <p>
 * The Cursor provides a variety of static utility methods to construct cursors with given characteristics or easily
 * search for specific values. For even friendlier and more flexible construction, see {@link CursorBuilder}.
 * <p>
 * Is not thread-safe.
 */
public abstract class CursorImpl implements Cursor {
    private static final Logger LOGGER         = System.getLogger(CursorImpl.class.getName());

    /** boolean value indicating forward movement */
    public static final boolean MOVE_FORWARD   = true;
    /** boolean value indicating reverse movement */
    public static final boolean MOVE_REVERSE   = false;

    /** identifier for this cursor */
    private final IdImpl        mid;
    /** owning table */
    private final TableImpl     mtable;
    /** State used for reading the table rows */
    private final RowState      mrowState;
    /** the first (exclusive) row id for this cursor */
    private final PositionImpl  mfirstPos;
    /** the last (exclusive) row id for this cursor */
    private final PositionImpl  mlastPos;
    /** the previous row */
    protected PositionImpl      mprevPos;
    /** the current row */
    protected PositionImpl      mcurPos;
    /** ColumnMatcher to be used when matching column values */
    protected ColumnMatcher     mcolumnMatcher = SimpleColumnMatcher.INSTANCE;

    protected CursorImpl(IdImpl id, TableImpl table, PositionImpl firstPos, PositionImpl lastPos) {
        mid = id;
        mtable = table;
        mrowState = mtable.createRowState();
        mfirstPos = firstPos;
        mlastPos = lastPos;
        mcurPos = firstPos;
        mprevPos = firstPos;
    }

    /**
     * Creates a normal, un-indexed cursor for the given table.
     *
     * @param table the table over which this cursor will traverse
     */
    public static CursorImpl createCursor(TableImpl table) {
        return new TableScanCursor(table);
    }

    public RowState getRowState() {
        return mrowState;
    }

    @Override
    public IdImpl getId() {
        return mid;
    }

    @Override
    public TableImpl getTable() {
        return mtable;
    }

    public JetFormat getFormat() {
        return getTable().getFormat();
    }

    public PageChannel getPageChannel() {
        return getTable().getPageChannel();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return mrowState.getErrorHandler();
    }

    @Override
    public void setErrorHandler(ErrorHandler newErrorHandler) {
        mrowState.setErrorHandler(newErrorHandler);
    }

    @Override
    public ColumnMatcher getColumnMatcher() {
        return mcolumnMatcher;
    }

    @Override
    public void setColumnMatcher(ColumnMatcher columnMatcher) {
        if (columnMatcher == null) {
            columnMatcher = getDefaultColumnMatcher();
        }
        mcolumnMatcher = columnMatcher;
    }

    /**
     * Returns the default ColumnMatcher for this Cursor.
     */
    protected ColumnMatcher getDefaultColumnMatcher() {
        return SimpleColumnMatcher.INSTANCE;
    }

    @Override
    public SavepointImpl getSavepoint() {
        return new SavepointImpl(mid, mcurPos, mprevPos);
    }

    @Override
    public void restoreSavepoint(Savepoint savepoint) throws IOException {
        restoreSavepoint((SavepointImpl) savepoint);
    }

    public void restoreSavepoint(SavepointImpl savepoint) throws IOException {
        if (!mid.equals(savepoint.getCursorId())) {
            throw new IllegalArgumentException("Savepoint " + savepoint + " is not valid for this cursor with id " + mid);
        }
        restorePosition(savepoint.getCurrentPosition(), savepoint.getPreviousPosition());
    }

    /**
     * Returns the first row id (exclusive) as defined by this cursor.
     */
    protected PositionImpl getFirstPosition() {
        return mfirstPos;
    }

    /**
     * Returns the last row id (exclusive) as defined by this cursor.
     */
    protected PositionImpl getLastPosition() {
        return mlastPos;
    }

    @Override
    public void reset() {
        beforeFirst();
    }

    @Override
    public void beforeFirst() {
        reset(MOVE_FORWARD);
    }

    @Override
    public void afterLast() {
        reset(MOVE_REVERSE);
    }

    @Override
    public boolean isBeforeFirst() throws IOException {
        return isAtBeginning(MOVE_FORWARD);
    }

    @Override
    public boolean isAfterLast() throws IOException {
        return isAtBeginning(MOVE_REVERSE);
    }

    protected boolean isAtBeginning(boolean moveForward) throws IOException {
        return getDirHandler(moveForward).getBeginningPosition().equals(mcurPos) && !recheckPosition(!moveForward);
    }

    @Override
    public boolean isCurrentRowDeleted() throws IOException {
        // we need to ensure that the "deleted" flag has been read for this row
        // (or re-read if the table has been recently modified)
        TableImpl.positionAtRowData(mrowState, mcurPos.getRowId());
        return mrowState.isDeleted();
    }

    /**
     * Resets this cursor for traversing the given direction.
     */
    protected void reset(boolean moveForward) {
        mcurPos = getDirHandler(moveForward).getBeginningPosition();
        mprevPos = mcurPos;
        mrowState.reset();
    }

    @Override
    public Iterator<Row> iterator() {
        return new RowIterator(null, true, MOVE_FORWARD);
    }

    @Override
    public IterableBuilder newIterable() {
        return new IterableBuilder(this);
    }

    public Iterator<Row> iterator(IterableBuilder iterBuilder) {

        switch (iterBuilder.getType()) {
            case SIMPLE:
                return new RowIterator(iterBuilder.getColumnNames(), iterBuilder.isReset(), iterBuilder.isForward());
            case COLUMN_MATCH:
                @SuppressWarnings("unchecked")
                Map.Entry<Column, Object> entry = (Map.Entry<Column, Object>) iterBuilder.getMatchPattern();
                return new ColumnMatchIterator(iterBuilder.getColumnNames(), (ColumnImpl) entry.getKey(), entry.getValue(), iterBuilder.isReset(), iterBuilder.isForward(),
                    iterBuilder.getColumnMatcher());
            case ROW_MATCH:
                @SuppressWarnings("unchecked")
                Map<String, ?> map = (Map<String, ?>) iterBuilder.getMatchPattern();
                return new RowMatchIterator(iterBuilder.getColumnNames(), map, iterBuilder.isReset(), iterBuilder.isForward(), iterBuilder.getColumnMatcher());
            default:
                throw new JackcessRuntimeException("Unknown match type " + iterBuilder.getType());
        }
    }

    @Override
    public void deleteCurrentRow() throws IOException {
        mtable.deleteRow(mrowState, mcurPos.getRowId());
    }

    @Override
    public Object[] updateCurrentRow(Object... row) throws IOException {
        return mtable.updateRow(mrowState, mcurPos.getRowId(), row);
    }

    @Override
    public <M extends Map<String, Object>> M updateCurrentRowFromMap(M row) throws IOException {
        return mtable.updateRowFromMap(mrowState, mcurPos.getRowId(), row);
    }

    @Override
    public Row getNextRow() throws IOException {
        return getNextRow(null);
    }

    @Override
    public Row getNextRow(Collection<String> columnNames) throws IOException {
        return getAnotherRow(columnNames, MOVE_FORWARD);
    }

    @Override
    public Row getPreviousRow() throws IOException {
        return getPreviousRow(null);
    }

    @Override
    public Row getPreviousRow(Collection<String> columnNames) throws IOException {
        return getAnotherRow(columnNames, MOVE_REVERSE);
    }

    /**
     * Moves to another row in the table based on the given direction and returns it.
     *
     * @param columnNames Only column names in this collection will be returned
     * @return another row in this table (Column name -&gt; Column value), where "next" may be backwards if moveForward
     *         is {@code false}, or {@code null} if there is not another row in the given direction.
     */
    private Row getAnotherRow(Collection<String> columnNames, boolean moveForward) throws IOException {
        if (moveToAnotherRow(moveForward)) {
            return getCurrentRow(columnNames);
        }
        return null;
    }

    @Override
    public boolean moveToNextRow() throws IOException {
        return moveToAnotherRow(MOVE_FORWARD);
    }

    @Override
    public boolean moveToPreviousRow() throws IOException {
        return moveToAnotherRow(MOVE_REVERSE);
    }

    /**
     * Moves to another row in the given direction as defined by this cursor.
     *
     * @return {@code true} if another valid row was found in the given direction, {@code false} otherwise
     */
    protected boolean moveToAnotherRow(boolean moveForward) throws IOException {
        if (mcurPos.equals(getDirHandler(moveForward).getEndPosition())) {
            // already at end, make sure nothing has changed
            return recheckPosition(moveForward);
        }

        return moveToAnotherRowImpl(moveForward);
    }

    /**
     * Restores a current position for the cursor (current position becomes previous position).
     */
    protected void restorePosition(PositionImpl curPos) throws IOException {
        restorePosition(curPos, mcurPos);
    }

    /**
     * Restores a current and previous position for the cursor if the given positions are different from the current
     * positions.
     */
    protected final void restorePosition(PositionImpl curPos, PositionImpl prevPos) throws IOException {
        if (!curPos.equals(mcurPos) || !prevPos.equals(mprevPos)) {
            restorePositionImpl(curPos, prevPos);
        }
    }

    /**
     * Restores a current and previous position for the cursor.
     */
    protected void restorePositionImpl(PositionImpl curPos, PositionImpl prevPos) throws IOException {
        // make the current position previous, and the new position current
        mprevPos = mcurPos;
        mcurPos = curPos;
        mrowState.reset();
    }

    /**
     * Rechecks the current position if the underlying data structures have been modified.
     *
     * @return {@code true} if the cursor ended up in a new position, {@code false} otherwise.
     */
    private boolean recheckPosition(boolean moveForward) throws IOException {
        if (isUpToDate()) {
            // nothing has changed
            return false;
        }

        // move the cursor back to the previous position
        restorePosition(mprevPos);
        return moveToAnotherRowImpl(moveForward);
    }

    /**
     * Does the grunt work of moving the cursor to another position in the given direction.
     */
    private boolean moveToAnotherRowImpl(boolean moveForward) throws IOException {
        mrowState.reset();
        mprevPos = mcurPos;
        mcurPos = findAnotherPosition(mrowState, mcurPos, moveForward);
        TableImpl.positionAtRowHeader(mrowState, mcurPos.getRowId());
        return !mcurPos.equals(getDirHandler(moveForward).getEndPosition());
    }

    @Override
    public boolean findRow(RowId rowId) throws IOException {
        RowIdImpl rowIdImpl = (RowIdImpl) rowId;
        PositionImpl curPos = mcurPos;
        PositionImpl prevPos = mprevPos;
        boolean found = false;
        try {
            reset(MOVE_FORWARD);
            if (TableImpl.positionAtRowHeader(mrowState, rowIdImpl) == null) {
                return false;
            }
            restorePosition(getRowPosition(rowIdImpl));
            if (!isCurrentRowValid()) {
                return false;
            }
            found = true;
            return true;
        } finally {
            if (!found) {
                try {
                    restorePosition(curPos, prevPos);
                } catch (IOException _ex) {
                    LOGGER.log(Level.ERROR, "Failed restoring position", _ex);
                }
            }
        }
    }

    @Override
    public boolean findFirstRow(Column columnPattern, Object valuePattern) throws IOException {
        return findFirstRow((ColumnImpl) columnPattern, valuePattern);
    }

    public boolean findFirstRow(ColumnImpl columnPattern, Object valuePattern) throws IOException {
        return findAnotherRow(columnPattern, valuePattern, true, MOVE_FORWARD, mcolumnMatcher, prepareSearchInfo(columnPattern, valuePattern));
    }

    @Override
    public boolean findNextRow(Column columnPattern, Object valuePattern) throws IOException {
        return findNextRow((ColumnImpl) columnPattern, valuePattern);
    }

    public boolean findNextRow(ColumnImpl columnPattern, Object valuePattern) throws IOException {
        return findAnotherRow(columnPattern, valuePattern, false, MOVE_FORWARD, mcolumnMatcher, prepareSearchInfo(columnPattern, valuePattern));
    }

    protected boolean findAnotherRow(ColumnImpl columnPattern, Object valuePattern, boolean reset, boolean moveForward, ColumnMatcher columnMatcher, Object searchInfo) throws IOException {
        PositionImpl curPos = mcurPos;
        PositionImpl prevPos = mprevPos;
        boolean found = false;
        try {
            if (reset) {
                reset(moveForward);
            }
            found = findAnotherRowImpl(columnPattern, valuePattern, moveForward, columnMatcher, searchInfo);
            return found;
        } finally {
            if (!found) {
                try {
                    restorePosition(curPos, prevPos);
                } catch (IOException _ex) {
                    LOGGER.log(Level.ERROR, "Failed restoring position", _ex);
                }
            }
        }
    }

    @Override
    public boolean findFirstRow(Map<String, ?> rowPattern) throws IOException {
        return findAnotherRow(rowPattern, true, MOVE_FORWARD, mcolumnMatcher, prepareSearchInfo(rowPattern));
    }

    @Override
    public boolean findNextRow(Map<String, ?> rowPattern) throws IOException {
        return findAnotherRow(rowPattern, false, MOVE_FORWARD, mcolumnMatcher, prepareSearchInfo(rowPattern));
    }

    protected boolean findAnotherRow(Map<String, ?> rowPattern, boolean reset, boolean moveForward, ColumnMatcher columnMatcher, Object searchInfo) throws IOException {
        PositionImpl curPos = mcurPos;
        PositionImpl prevPos = mprevPos;
        boolean found = false;
        try {
            if (reset) {
                reset(moveForward);
            }
            found = findAnotherRowImpl(rowPattern, moveForward, columnMatcher, searchInfo);
            return found;
        } finally {
            if (!found) {
                try {
                    restorePosition(curPos, prevPos);
                } catch (IOException _ex) {
                    LOGGER.log(Level.ERROR, "Failed restoring position", _ex);
                }
            }
        }
    }

    @Override
    public boolean currentRowMatches(Column columnPattern, Object valuePattern) throws IOException {
        return currentRowMatches((ColumnImpl) columnPattern, valuePattern);
    }

    public boolean currentRowMatches(ColumnImpl columnPattern, Object valuePattern) throws IOException {
        return currentRowMatchesImpl(columnPattern, valuePattern, mcolumnMatcher);
    }

    protected boolean currentRowMatchesImpl(ColumnImpl columnPattern, Object valuePattern, ColumnMatcher columnMatcher) throws IOException {
        return currentRowMatchesPattern(columnPattern.getName(), valuePattern, columnMatcher, getCurrentRowValue(columnPattern));
    }

    @Override
    public boolean currentRowMatches(Map<String, ?> rowPattern) throws IOException {
        return currentRowMatchesImpl(rowPattern, mcolumnMatcher);
    }

    protected boolean currentRowMatchesImpl(Map<String, ?> rowPattern, ColumnMatcher columnMatcher) throws IOException {
        Row row = getCurrentRow(rowPattern.keySet());

        if (rowPattern.size() != row.size()) {
            return false;
        }

        for (Map.Entry<String, Object> e : row.entrySet()) {
            String columnName = e.getKey();
            if (!currentRowMatchesPattern(columnName, rowPattern.get(columnName), columnMatcher, e.getValue())) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    protected final boolean currentRowMatchesPattern(String columnPattern, Object valuePattern, ColumnMatcher columnMatcher, Object rowValue) {
        // if the value pattern is a Predicate use that to test the value
        if (valuePattern instanceof Predicate<?>) {
            return ((Predicate<Object>) valuePattern).test(rowValue);
        }
        // otherwise, use the configured ColumnMatcher
        return columnMatcher.matches(getTable(), columnPattern, valuePattern, rowValue);
    }

    /**
     * Moves to the next row (as defined by the cursor) where the given column has the given value. Caller manages
     * save/restore on failure.
     * <p>
     * Default implementation scans the table from beginning to end.
     *
     * @param columnPattern column from the table for this cursor which is being matched by the valuePattern
     * @param valuePattern value which is equal to the corresponding value in the matched row
     * @return {@code true} if a valid row was found with the given value, {@code false} if no row was found
     */
    protected boolean findAnotherRowImpl(ColumnImpl columnPattern, Object valuePattern, boolean moveForward, ColumnMatcher columnMatcher, Object searchInfo) throws IOException {
        while (moveToAnotherRow(moveForward)) {
            if (currentRowMatchesImpl(columnPattern, valuePattern, columnMatcher)) {
                return true;
            }
            if (!keepSearching(columnMatcher, searchInfo)) {
                break;
            }
        }
        return false;
    }

    /**
     * Moves to the next row (as defined by the cursor) where the given columns have the given values. Caller manages
     * save/restore on failure.
     * <p>
     * Default implementation scans the table from beginning to end.
     *
     * @param rowPattern column names and values which must be equal to the corresponding values in the matched row
     * @return {@code true} if a valid row was found with the given values, {@code false} if no row was found
     */
    protected boolean findAnotherRowImpl(Map<String, ?> rowPattern, boolean moveForward, ColumnMatcher columnMatcher, Object searchInfo) throws IOException {
        while (moveToAnotherRow(moveForward)) {
            if (currentRowMatchesImpl(rowPattern, columnMatcher)) {
                return true;
            }
            if (!keepSearching(columnMatcher, searchInfo)) {
                break;
            }
        }
        return false;
    }

    /**
     * Called before a search commences to allow for search specific data to be generated (which is cached for re-use by
     * the iterators).
     */
    protected Object prepareSearchInfo(ColumnImpl columnPattern, Object valuePattern) {
        return null;
    }

    /**
     * Called before a search commences to allow for search specific data to be generated (which is cached for re-use by
     * the iterators).
     */
    protected Object prepareSearchInfo(Map<String, ?> rowPattern) {
        return null;
    }

    /**
     * Called by findAnotherRowImpl to determine if the search should continue after finding a row which does not match
     * the current pattern.
     */
    protected boolean keepSearching(ColumnMatcher columnMatcher, Object searchInfo) throws IOException {
        return true;
    }

    @Override
    public int moveNextRows(int numRows) throws IOException {
        return moveSomeRows(numRows, MOVE_FORWARD);
    }

    @Override
    public int movePreviousRows(int numRows) throws IOException {
        return moveSomeRows(numRows, MOVE_REVERSE);
    }

    /**
     * Moves as many rows as possible in the given direction up to the given number of rows.
     *
     * @return the number of rows moved.
     */
    private int moveSomeRows(int numRows, boolean moveForward) throws IOException {
        int numMovedRows = 0;
        while (numMovedRows < numRows && moveToAnotherRow(moveForward)) {
            numMovedRows++;
        }
        return numMovedRows;
    }

    @Override
    public Row getCurrentRow() throws IOException {
        return getCurrentRow(null);
    }

    @Override
    public Row getCurrentRow(Collection<String> columnNames) throws IOException {
        return mtable.getRow(mrowState, mcurPos.getRowId(), columnNames);
    }

    @Override
    public Object getCurrentRowValue(Column column) throws IOException {
        return getCurrentRowValue((ColumnImpl) column);
    }

    public Object getCurrentRowValue(ColumnImpl column) throws IOException {
        return mtable.getRowValue(mrowState, mcurPos.getRowId(), column);
    }

    @Override
    public void setCurrentRowValue(Column column, Object value) throws IOException {
        setCurrentRowValue((ColumnImpl) column, value);
    }

    public void setCurrentRowValue(ColumnImpl column, Object value) throws IOException {
        Object[] row = new Object[mtable.getColumnCount()];
        Arrays.fill(row, Column.KEEP_VALUE);
        column.setRowValue(row, value);
        mtable.updateRow(mrowState, mcurPos.getRowId(), row);
    }

    /**
     * Returns {@code true} if this cursor is up-to-date with respect to the relevant table and related table objects,
     * {@code false} otherwise.
     */
    protected boolean isUpToDate() {
        return mrowState.isUpToDate();
    }

    /**
     * Returns {@code true} of the current row is valid, {@code false} otherwise.
     */
    protected boolean isCurrentRowValid() throws IOException {
        return mcurPos.getRowId().isValid() && !isCurrentRowDeleted() && !isBeforeFirst() && !isAfterLast();
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, table=%s, prevPos=%s, curPos=%s]", getClass().getSimpleName(), mid, mtable, mprevPos, mcurPos);
    }

    /**
     * Returns the appropriate position information for the given row (which is the current row and is valid).
     */
    protected abstract PositionImpl getRowPosition(RowIdImpl rowId) throws IOException;

    /**
     * Finds the next non-deleted row after the given row (as defined by this cursor) and returns the id of the row,
     * where "next" may be backwards if moveForward is {@code false}. If there are no more rows, the returned rowId
     * should equal the value returned by {@link #getLastPosition} if moving forward and {@link #getFirstPosition} if
     * moving backward.
     */
    protected abstract PositionImpl findAnotherPosition(RowState rowState, PositionImpl curPos, boolean moveForward) throws IOException;

    /**
     * Returns the DirHandler for the given movement direction.
     */
    protected abstract DirHandler getDirHandler(boolean moveForward);

    /**
     * Base implementation of iterator for this cursor, modifiable.
     */
    protected abstract class BaseIterator implements Iterator<Row> {
        protected final Collection<String> _columnNames;
        protected final boolean            _moveForward;
        protected final ColumnMatcher      _colMatcher;
        protected Boolean                  _hasNext;
        protected boolean                  _validRow;

        protected BaseIterator(Collection<String> columnNames, boolean reset, boolean moveForward, ColumnMatcher columnMatcher) {
            _columnNames = columnNames;
            _moveForward = moveForward;
            _colMatcher = columnMatcher != null ? columnMatcher : mcolumnMatcher;
            try {
                if (reset) {
                    reset(_moveForward);
                } else if (isCurrentRowValid()) {
                    _hasNext = _validRow = true;
                }
            } catch (IOException _ex) {
                throw new UncheckedIOException(_ex);
            }
        }

        @Override
        public boolean hasNext() {
            if (_hasNext == null) {
                try {
                    _hasNext = findNext();
                    _validRow = _hasNext;
                } catch (IOException _ex) {
                    throw new UncheckedIOException(_ex);
                }
            }
            return _hasNext;
        }

        @Override
        public Row next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                Row rtn = getCurrentRow(_columnNames);
                _hasNext = null;
                return rtn;
            } catch (IOException _ex) {
                throw new UncheckedIOException(_ex);
            }
        }

        @Override
        public void remove() {
            if (_validRow) {
                try {
                    deleteCurrentRow();
                    _validRow = false;
                } catch (IOException _ex) {
                    throw new UncheckedIOException(_ex);
                }
            } else {
                throw new IllegalStateException("Not at valid row");
            }
        }

        protected abstract boolean findNext() throws IOException;
    }

    /**
     * Row iterator for this cursor, modifiable.
     */
    private final class RowIterator extends BaseIterator {
        private RowIterator(Collection<String> columnNames, boolean reset, boolean moveForward) {
            super(columnNames, reset, moveForward, null);
        }

        @Override
        protected boolean findNext() throws IOException {
            return moveToAnotherRow(_moveForward);
        }
    }

    /**
     * Row iterator for this cursor, modifiable.
     */
    private final class ColumnMatchIterator extends BaseIterator {
        private final ColumnImpl _columnPattern;
        private final Object     _valuePattern;
        private final Object     _searchInfo;

        private ColumnMatchIterator(Collection<String> columnNames, ColumnImpl columnPattern, Object valuePattern, boolean reset, boolean moveForward, ColumnMatcher columnMatcher) {
            super(columnNames, reset, moveForward, columnMatcher);
            _columnPattern = columnPattern;
            _valuePattern = valuePattern;
            _searchInfo = prepareSearchInfo(columnPattern, valuePattern);
        }

        @Override
        protected boolean findNext() throws IOException {
            return findAnotherRow(_columnPattern, _valuePattern, false, _moveForward, _colMatcher, _searchInfo);
        }
    }

    /**
     * Row iterator for this cursor, modifiable.
     */
    private final class RowMatchIterator extends BaseIterator {
        private final Map<String, ?> _rowPattern;
        private final Object         _searchInfo;

        private RowMatchIterator(Collection<String> columnNames, Map<String, ?> rowPattern, boolean reset, boolean moveForward, ColumnMatcher columnMatcher) {
            super(columnNames, reset, moveForward, columnMatcher);
            _rowPattern = rowPattern;
            _searchInfo = prepareSearchInfo(rowPattern);
        }

        @Override
        protected boolean findNext() throws IOException {
            return findAnotherRow(_rowPattern, false, _moveForward, _colMatcher, _searchInfo);
        }
    }

    /**
     * Handles moving the cursor in a given direction. Separates cursor logic from value storage.
     */
    protected abstract static class DirHandler {
        public abstract PositionImpl getBeginningPosition();

        public abstract PositionImpl getEndPosition();
    }

    /**
     * Identifier for a cursor. Will be equal to any other cursor of the same type for the same table. Primarily used to
     * check the validity of a Savepoint.
     */
    protected static final class IdImpl implements Id {
        private final int _tablePageNumber;
        private final int _indexNumber;

        protected IdImpl(TableImpl table, IndexImpl index) {
            _tablePageNumber = table.getTableDefPageNumber();
            _indexNumber = index != null ? index.getIndexNumber() : -1;
        }

        @Override
        public int hashCode() {
            return _tablePageNumber;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && _tablePageNumber == ((IdImpl) o)._tablePageNumber && _indexNumber == ((IdImpl) o)._indexNumber;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " + _tablePageNumber + ":" + _indexNumber;
        }
    }

    /**
     * Value object which maintains the current position of the cursor.
     */
    protected abstract static class PositionImpl implements Position {
        protected PositionImpl() {
        }

        @Override
        public final int hashCode() {
            return getRowId().hashCode();
        }

        @Override
        public final boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && equalsImpl(o);
        }

        /**
         * Returns the unique RowId of the position of the cursor.
         */
        @Override
        public abstract RowIdImpl getRowId();

        /**
         * Returns {@code true} if the subclass specific info in a Position is equal, {@code false} otherwise.
         *
         * @param o object being tested for equality, guaranteed to be the same class as this object
         */
        protected abstract boolean equalsImpl(Object o);
    }

    /**
     * Value object which represents a complete save state of the cursor.
     */
    protected static final class SavepointImpl implements Savepoint {
        private final IdImpl       _cursorId;
        private final PositionImpl _curPos;
        private final PositionImpl _prevPos;

        private SavepointImpl(IdImpl cursorId, PositionImpl curPos, PositionImpl prevPos) {
            _cursorId = cursorId;
            _curPos = curPos;
            _prevPos = prevPos;
        }

        @Override
        public IdImpl getCursorId() {
            return _cursorId;
        }

        @Override
        public PositionImpl getCurrentPosition() {
            return _curPos;
        }

        private PositionImpl getPreviousPosition() {
            return _prevPos;
        }

        @Override
        public String toString() {
            return String.format("%s[cursorId=%s, curPos=%s, prevPos=%s]", getClass().getSimpleName(), _cursorId, _curPos, _prevPos);
        }
    }

}
