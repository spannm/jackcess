/*
Copyright (c) 2011 James Ahlborn

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

package com.healthmarketscience.jackcess.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.IndexCursor;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.RuntimeIOException;
import com.healthmarketscience.jackcess.impl.TableImpl.RowState;
import com.healthmarketscience.jackcess.util.CaseInsensitiveColumnMatcher;
import com.healthmarketscience.jackcess.util.ColumnMatcher;
import com.healthmarketscience.jackcess.util.EntryIterableBuilder;
import com.healthmarketscience.jackcess.util.SimpleColumnMatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Cursor backed by an index with extended traversal options.
 *
 * @author James Ahlborn
 */
public class IndexCursorImpl extends CursorImpl implements IndexCursor
{
  private static final Log LOG = LogFactory.getLog(IndexCursorImpl.class);

  /** IndexDirHandler for forward traversal */
  private final IndexDirHandler _forwardDirHandler =
    new ForwardIndexDirHandler();
  /** IndexDirHandler for backward traversal */
  private final IndexDirHandler _reverseDirHandler =
    new ReverseIndexDirHandler();
  /** logical index which this cursor is using */
  private final IndexImpl _index;
  /** Cursor over the entries of the relevant index */
  private final IndexData.EntryCursor _entryCursor;
  /** column names for the index entry columns */
  private Set<String> _indexEntryPattern;

  private IndexCursorImpl(TableImpl table, IndexImpl index,
                          IndexData.EntryCursor entryCursor)
    throws IOException
  {
    super(new IdImpl(table, index), table,
          new IndexPosition(entryCursor.getFirstEntry()),
          new IndexPosition(entryCursor.getLastEntry()));
    _index = index;
    _index.initialize();
    _entryCursor = entryCursor;
  }

  /**
   * Creates an indexed cursor for the given table, narrowed to the given
   * range.
   * <p>
   * Note, index based table traversal may not include all rows, as certain
   * types of indexes do not include all entries (namely, some indexes ignore
   * null entries, see {@link Index#shouldIgnoreNulls}).
   *
   * @param table the table over which this cursor will traverse
   * @param index index for the table which will define traversal order as
   *              well as enhance certain lookups
   * @param startRow the first row of data for the cursor, or {@code null} for
   *                 the first entry
   * @param startInclusive whether or not startRow is inclusive or exclusive
   * @param endRow the last row of data for the cursor, or {@code null} for
   *               the last entry
   * @param endInclusive whether or not endRow is inclusive or exclusive
   */
  public static IndexCursorImpl createCursor(TableImpl table, IndexImpl index,
                                             Object[] startRow,
                                             boolean startInclusive,
                                             Object[] endRow,
                                             boolean endInclusive)
    throws IOException
  {
    if(table != index.getTable()) {
      throw new IllegalArgumentException(
          "Given index is not for given table: " + index + ", " + table);
    }
    if(index.getIndexData().getUnsupportedReason() != null) {
      throw new IllegalArgumentException(
          "Given index " + index +
          " is not usable for indexed lookups due to " +
          index.getIndexData().getUnsupportedReason());
    }
    IndexCursorImpl cursor = new IndexCursorImpl(
        table, index, index.cursor(startRow, startInclusive,
                                   endRow, endInclusive));
    // init the column matcher appropriately for the index type
    cursor.setColumnMatcher(null);
    return cursor;
  }

  private Set<String> getIndexEntryPattern()
  {
    if(_indexEntryPattern == null) {
      // init our set of index column names
      _indexEntryPattern = new HashSet<String>();
      for(IndexData.ColumnDescriptor col : getIndex().getColumns()) {
        _indexEntryPattern.add(col.getName());
      }
    }
    return _indexEntryPattern;
  }

  @Override
  public IndexImpl getIndex() {
    return _index;
  }

  @Override
  public Row findRowByEntry(Object... entryValues)
    throws IOException
  {
    if(findFirstRowByEntry(entryValues)) {
      return getCurrentRow();
    }
    return null;
  }

  @Override
  public boolean findFirstRowByEntry(Object... entryValues)
    throws IOException
  {
    PositionImpl curPos = _curPos;
    PositionImpl prevPos = _prevPos;
    boolean found = false;
    try {
      found = findFirstRowByEntryImpl(toRowValues(entryValues), true,
                                      _columnMatcher);
      return found;
    } finally {
      if(!found) {
        try {
          restorePosition(curPos, prevPos);
        } catch(IOException e) {
          LOG.error("Failed restoring position", e);
        }
      }
    }
  }

  @Override
  public void findClosestRowByEntry(Object... entryValues)
    throws IOException
  {
    PositionImpl curPos = _curPos;
    PositionImpl prevPos = _prevPos;
    boolean found = false;
    try {
      findFirstRowByEntryImpl(toRowValues(entryValues), false,
                              _columnMatcher);
      found = true;
    } finally {
      if(!found) {
        try {
          restorePosition(curPos, prevPos);
        } catch(IOException e) {
          LOG.error("Failed restoring position", e);
        }
      }
    }
  }

  @Override
  public boolean currentRowMatchesEntry(Object... entryValues)
    throws IOException
  {
    return currentRowMatchesEntryImpl(toRowValues(entryValues), _columnMatcher);
  }

  @Override
  public EntryIterableBuilder newEntryIterable(Object... entryValues) {
    return new EntryIterableBuilder(this, entryValues);
  }

  public Iterator<Row> entryIterator(EntryIterableBuilder iterBuilder) {
    return new EntryIterator(iterBuilder.getColumnNames(),
                             toRowValues(iterBuilder.getEntryValues()),
                             iterBuilder.getColumnMatcher());
  }

  @Override
  protected IndexDirHandler getDirHandler(boolean moveForward) {
    return (moveForward ? _forwardDirHandler : _reverseDirHandler);
  }

  @Override
  protected boolean isUpToDate() {
    return(super.isUpToDate() && _entryCursor.isUpToDate());
  }

  @Override
  protected void reset(boolean moveForward) {
    _entryCursor.reset(moveForward);
    super.reset(moveForward);
  }

  @Override
  protected void restorePositionImpl(PositionImpl curPos, PositionImpl prevPos)
    throws IOException
  {
    if(!(curPos instanceof IndexPosition) ||
       !(prevPos instanceof IndexPosition)) {
      throw new IllegalArgumentException(
          "Restored positions must be index positions");
    }
    _entryCursor.restorePosition(((IndexPosition)curPos).getEntry(),
                                 ((IndexPosition)prevPos).getEntry());
    super.restorePositionImpl(curPos, prevPos);
  }

  @Override
  protected PositionImpl getRowPosition(RowIdImpl rowId) throws IOException
  {
    // we need to get the index entry which corresponds with this row
    Row row = getTable().getRow(getRowState(), rowId, getIndexEntryPattern());
    _entryCursor.beforeEntry(getTable().asRow(row));
    return new IndexPosition(_entryCursor.getNextEntry());
  }

  @Override
  protected boolean findAnotherRowImpl(
      ColumnImpl columnPattern, Object valuePattern, boolean moveForward,
      ColumnMatcher columnMatcher, Object searchInfo)
    throws IOException
  {
    Object[] rowValues = (Object[])searchInfo;

    if((rowValues == null) || !isAtBeginning(moveForward)) {
      // use the default table scan if we don't have index data or we are
      // mid-cursor
      return super.findAnotherRowImpl(columnPattern, valuePattern, moveForward,
                                      columnMatcher, rowValues);
    }

    // sweet, we can use our index
    if(!findPotentialRow(rowValues, true)) {
      return false;
    }

    // either we found a row with the given value, or none exist in the table
    return currentRowMatchesImpl(columnPattern, valuePattern, columnMatcher);
  }

  /**
   * Moves to the first row (as defined by the cursor) where the index entries
   * match the given values.  Caller manages save/restore on failure.
   *
   * @param rowValues the column values built from the index column values
   * @param requireMatch whether or not an exact match is desired
   * @return {@code true} if a valid row was found with the given values,
   *         {@code false} if no row was found
   */
  protected boolean findFirstRowByEntryImpl(Object[] rowValues,
                                            boolean requireMatch,
                                            ColumnMatcher columnMatcher)
    throws IOException
  {
    if(!findPotentialRow(rowValues, requireMatch)) {
      return false;
    } else if(!requireMatch) {
      // nothing more to do, we have moved to the closest row
      return true;
    }

    return currentRowMatchesEntryImpl(rowValues, columnMatcher);
  }

  @Override
  protected boolean findAnotherRowImpl(
      Map<String,?> rowPattern, boolean moveForward,
      ColumnMatcher columnMatcher, Object searchInfo)
    throws IOException
  {
    Object[] rowValues = (Object[])searchInfo;

    if((rowValues == null) || !isAtBeginning(moveForward)) {
      // use the default table scan if we don't have index data or we are
      // mid-cursor
      return super.findAnotherRowImpl(rowPattern, moveForward, columnMatcher,
                                      rowValues);
    }

    // sweet, we can use our index
    if(!findPotentialRow(rowValues, true)) {
      // at end of index, no potential matches
      return false;
    }

    // determine if the pattern columns exactly match the index columns
    boolean exactColumnMatch = rowPattern.keySet().equals(
        getIndexEntryPattern());

    // there may be multiple rows which fit the pattern subset used by
    // the index, so we need to keep checking until our index values no
    // longer match
    do {

      if(!currentRowMatchesEntryImpl(rowValues, columnMatcher)) {
        // there are no more rows which could possibly match
        break;
      }

      // note, if exactColumnMatch, no need to do an extra comparison with the
      // current row (since the entry match check above is equivalent to this
      // check)
      if(exactColumnMatch || currentRowMatchesImpl(rowPattern, columnMatcher)) {
        // found it!
        return true;
      }

    } while(moveToAnotherRow(moveForward));

    // none of the potential rows matched
    return false;
  }

  private boolean currentRowMatchesEntryImpl(Object[] rowValues,
                                             ColumnMatcher columnMatcher)
    throws IOException
  {
    // check the next row to see if it actually matches
    Row row = getCurrentRow(getIndexEntryPattern());

    for(IndexData.ColumnDescriptor col : getIndex().getColumns()) {

      Object patValue = rowValues[col.getColumnIndex()];

      if((patValue == IndexData.MIN_VALUE) ||
         (patValue == IndexData.MAX_VALUE)) {
        // all remaining entry values are "special" (used for partial lookups)
        return true;
      }

      String columnName = col.getName();
      Object rowValue = row.get(columnName);
      if(!columnMatcher.matches(getTable(), columnName, patValue, rowValue)) {
        return false;
      }
    }

    return true;
  }

  private boolean findPotentialRow(Object[] rowValues, boolean requireMatch)
    throws IOException
  {
    _entryCursor.beforeEntry(rowValues);
    IndexData.Entry startEntry = _entryCursor.getNextEntry();
    if(requireMatch && !startEntry.getRowId().isValid()) {
      // at end of index, no potential matches
      return false;
    }
    // move to position and check it out
    restorePosition(new IndexPosition(startEntry));
    return true;
  }

  @Override
  protected Object prepareSearchInfo(ColumnImpl columnPattern, Object valuePattern)
  {
    // attempt to generate a lookup row for this index
    return _entryCursor.getIndexData().constructPartialIndexRow(
        IndexData.MIN_VALUE, columnPattern.getName(), valuePattern);
  }

  @Override
  protected Object prepareSearchInfo(Map<String,?> rowPattern)
  {
    // attempt to generate a lookup row for this index
    return _entryCursor.getIndexData().constructPartialIndexRow(
        IndexData.MIN_VALUE, rowPattern);
  }

  @Override
  protected boolean keepSearching(ColumnMatcher columnMatcher,
                                  Object searchInfo)
    throws IOException
  {
    if(searchInfo instanceof Object[]) {
      // if we have a lookup row for this index, then we only need to continue
      // searching while we are looking at rows which match the index lookup
      // value(s).  once we move past those rows, no other rows could possibly
      // match.
      return currentRowMatchesEntryImpl((Object[])searchInfo, columnMatcher);
    }
    // we are doing a full table scan
    return true;
  }

  private Object[] toRowValues(Object[] entryValues)
  {
    return _entryCursor.getIndexData().constructPartialIndexRowFromEntry(
        IndexData.MIN_VALUE, entryValues);
  }

  @Override
  protected PositionImpl findAnotherPosition(
      RowState rowState, PositionImpl curPos, boolean moveForward)
    throws IOException
  {
    IndexDirHandler handler = getDirHandler(moveForward);
    IndexPosition endPos = (IndexPosition)handler.getEndPosition();
    IndexData.Entry entry = handler.getAnotherEntry();
    return ((!entry.equals(endPos.getEntry())) ?
            new IndexPosition(entry) : endPos);
  }

  @Override
  protected ColumnMatcher getDefaultColumnMatcher() {
    if(getIndex().isUnique()) {
      // text indexes are case-insensitive, therefore we should always use a
      // case-insensitive matcher for unique indexes.
      return CaseInsensitiveColumnMatcher.INSTANCE;
    }
    return SimpleColumnMatcher.INSTANCE;
  }

  /**
   * Handles moving the table index cursor in a given direction.  Separates
   * cursor logic from value storage.
   */
  private abstract class IndexDirHandler extends DirHandler {
    public abstract IndexData.Entry getAnotherEntry()
      throws IOException;
  }

  /**
   * Handles moving the table index cursor forward.
   */
  private final class ForwardIndexDirHandler extends IndexDirHandler {
    @Override
    public PositionImpl getBeginningPosition() {
      return getFirstPosition();
    }
    @Override
    public PositionImpl getEndPosition() {
      return getLastPosition();
    }
    @Override
    public IndexData.Entry getAnotherEntry() throws IOException {
      return _entryCursor.getNextEntry();
    }
  }

  /**
   * Handles moving the table index cursor backward.
   */
  private final class ReverseIndexDirHandler extends IndexDirHandler {
    @Override
    public PositionImpl getBeginningPosition() {
      return getLastPosition();
    }
    @Override
    public PositionImpl getEndPosition() {
      return getFirstPosition();
    }
    @Override
    public IndexData.Entry getAnotherEntry() throws IOException {
      return _entryCursor.getPreviousEntry();
    }
  }

  /**
   * Value object which maintains the current position of an IndexCursor.
   */
  private static final class IndexPosition extends PositionImpl
  {
    private final IndexData.Entry _entry;

    private IndexPosition(IndexData.Entry entry) {
      _entry = entry;
    }

    @Override
    public RowIdImpl getRowId() {
      return getEntry().getRowId();
    }

    public IndexData.Entry getEntry() {
      return _entry;
    }

    @Override
    protected boolean equalsImpl(Object o) {
      return getEntry().equals(((IndexPosition)o).getEntry());
    }

    @Override
    public String toString() {
      return "Entry = " + getEntry();
    }
  }

  /**
   * Row iterator (by matching entry) for this cursor, modifiable.
   */
  private final class EntryIterator extends BaseIterator
  {
    private final Object[] _rowValues;

    private EntryIterator(Collection<String> columnNames, Object[] rowValues,
                          ColumnMatcher columnMatcher)
    {
      super(columnNames, false, MOVE_FORWARD, columnMatcher);
      _rowValues = rowValues;
      try {
        _hasNext = findFirstRowByEntryImpl(rowValues, true, _columnMatcher);
        _validRow = _hasNext;
      } catch(IOException e) {
          throw new RuntimeIOException(e);
      }
    }

    @Override
    protected boolean findNext() throws IOException {
      return (moveToNextRow() &&
              currentRowMatchesEntryImpl(_rowValues, _colMatcher));
    }
  }

}
