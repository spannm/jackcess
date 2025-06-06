package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.IndexImpl;

import java.io.IOException;
import java.util.*;

/**
 * Utility for finding rows based on pre-defined, foreign-key table relationships.
 */
public class Joiner {
    private final Index                        _fromIndex;
    private final List<? extends Index.Column> _fromCols;
    private final IndexCursor                  _toCursor;
    private final Object[]                     _entryValues;

    private Joiner(Index fromIndex, IndexCursor toCursor) {
        _fromIndex = fromIndex;
        _fromCols = _fromIndex.getColumns();
        _entryValues = new Object[_fromCols.size()];
        _toCursor = toCursor;
    }

    /**
     * Creates a new Joiner based on the foreign-key relationship between the given "from"" table and the given "to""
     * table.
     *
     * @param fromTable the "from" side of the relationship
     * @param toTable the "to" side of the relationship
     * @throws IllegalArgumentException if there is no relationship between the given tables
     */
    public static Joiner create(Table fromTable, Table toTable) throws IOException {
        return create(fromTable.getForeignKeyIndex(toTable));
    }

    /**
     * Creates a new Joiner based on the given index which backs a foreign-key relationship. The table of the given
     * index will be the "from" table and the table on the other end of the relationship will be the "to" table.
     *
     * @param fromIndex the index backing one side of a foreign-key relationship
     */
    public static Joiner create(Index fromIndex) throws IOException {
        Index toIndex = fromIndex.getReferencedIndex();
        IndexCursor toCursor = CursorBuilder.createCursor(toIndex);
        // text lookups are always case-insensitive
        toCursor.setColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE);
        return new Joiner(fromIndex, toCursor);
    }

    /**
     * Creates a new Joiner that is the reverse of this Joiner (the "from" and "to" tables are swapped).
     */
    public Joiner createReverse() throws IOException {
        return create(getToTable(), getFromTable());
    }

    public Table getFromTable() {
        return getFromIndex().getTable();
    }

    public Index getFromIndex() {
        return _fromIndex;
    }

    public Table getToTable() {
        return getToCursor().getTable();
    }

    public Index getToIndex() {
        return getToCursor().getIndex();
    }

    public IndexCursor getToCursor() {
        return _toCursor;
    }

    public List<? extends Index.Column> getColumns() {
        // note, this list is already unmodifiable, no need to re-wrap
        return _fromCols;
    }

    /**
     * Returns {@code true} if the "to" table has any rows based on the given columns in the "from" table, {@code false}
     * otherwise.
     */
    public boolean hasRows(Map<String, ?> fromRow) throws IOException {
        toEntryValues(fromRow);
        return _toCursor.findFirstRowByEntry(_entryValues);
    }

    /**
     * Returns {@code true} if the "to" table has any rows based on the given columns in the "from" table, {@code false}
     * otherwise.
     */
    public boolean hasRows(Object[] fromRow) throws IOException {
        toEntryValues(fromRow);
        return _toCursor.findFirstRowByEntry(_entryValues);
    }

    /**
     * Returns the first row in the "to" table based on the given columns in the "from" table if any, {@code null} if
     * there is no matching row.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     */
    public Row findFirstRow(Map<String, ?> fromRow) throws IOException {
        return findFirstRow(fromRow, null);
    }

    /**
     * Returns selected columns from the first row in the "to" table based on the given columns in the "from" table if
     * any, {@code null} if there is no matching row.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     * @param columnNames desired columns in the from table row
     */
    public Row findFirstRow(Map<String, ?> fromRow, Collection<String> columnNames) throws IOException {
        return hasRows(fromRow) ? _toCursor.getCurrentRow(columnNames) : null;
    }

    /**
     * Returns an Iterator over all the rows in the "to" table based on the given columns in the "from" table.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     */
    public EntryIterableBuilder findRows(Map<String, ?> fromRow) {
        toEntryValues(fromRow);
        return _toCursor.newEntryIterable(_entryValues);
    }

    /**
     * Returns an Iterator with the selected columns over all the rows in the "to" table based on the given columns in
     * the "from" table.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     */
    public EntryIterableBuilder findRows(Object[] fromRow) {
        toEntryValues(fromRow);
        return _toCursor.newEntryIterable(_entryValues);
    }

    /**
     * Deletes any rows in the "to" table based on the given columns in the "from" table.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     * @return {@code true} if any "to" rows were deleted, {@code false} otherwise
     */
    public boolean deleteRows(Map<String, ?> fromRow) {
        return deleteRowsImpl(findRows(fromRow).withColumnNames(Set.of()).iterator());
    }

    /**
     * Deletes any rows in the "to" table based on the given columns in the "from" table.
     *
     * @param fromRow row from the "from" table (which must include the relevant columns for this join relationship)
     * @return {@code true} if any "to" rows were deleted, {@code false} otherwise
     */
    public boolean deleteRows(Object[] fromRow) {
        return deleteRowsImpl(findRows(fromRow).withColumnNames(Set.of()).iterator());
    }

    /**
     * Deletes all the rows and returns whether or not any "to"" rows were deleted.
     */
    private static boolean deleteRowsImpl(Iterator<Row> iter) {
        boolean removed = false;
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
            removed = true;
        }
        return removed;
    }

    /**
     * Fills in the _entryValues with the relevant info from the given "from" table row.
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    private void toEntryValues(Map<String, ?> fromRow) {
        for (int i = 0; i < _entryValues.length; ++i) {
            _entryValues[i] = _fromCols.get(i).getColumn().getRowValue(fromRow);
        }
    }

    /**
     * Fills in the _entryValues with the relevant info from the given "from" table row.
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    private void toEntryValues(Object[] fromRow) {
        for (int i = 0; i < _entryValues.length; ++i) {
            _entryValues[i] = _fromCols.get(i).getColumn().getRowValue(fromRow);
        }
    }

    /**
     * Returns a pretty string describing the foreign key relationship backing this Joiner.
     */
    public String toFKString() {
        StringBuilder sb = new StringBuilder("Foreign Key from ");

        String fromType = "] (primary)";
        String toType = "] (secondary)";
        if (!((IndexImpl) _fromIndex).getReference().isPrimaryTable()) {
            fromType = "] (secondary)";
            toType = "] (primary)";
        }

        sb.append(getFromTable().getName())
          .append('[')

          .append(_fromCols.get(0).getName());
        for (int i = 1; i < _fromCols.size(); ++i) {
            sb.append(',').append(_fromCols.get(i).getName());
        }
        sb.append(fromType)

          .append(" to ")
          .append(getToTable().getName())
          .append('[');
        List<? extends Index.Column> toCols = _toCursor.getIndex().getColumns();
        sb.append(toCols.get(0).getName());
        for (int i = 1; i < toCols.size(); ++i) {
            sb.append(',').append(toCols.get(i).getName());
        }
        return sb.append(toType)
          .append(" (Db=")
          .append(((DatabaseImpl) getFromTable().getDatabase()).getName())
          .append(')')
          .toString();
    }
}
