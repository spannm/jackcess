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

package com.healthmarketscience.jackcess.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Row;


/**
 * The RowFilter class encapsulates a filter test for a table row.  This can
 * be used by the {@link #apply(Iterable)} method to create an Iterable over a
 * table which returns only rows matching some criteria.
 *
 * @author Patricia Donaldson, Xerox Corporation
 * @usage _general_class_
 */
public abstract class RowFilter implements Predicate<Row>
{

  /**
   * Returns {@code true} if the given table row matches the Filter criteria,
   * {@code false} otherwise.
   * @param row current row to test for inclusion in the filter
   */
  public abstract boolean matches(Row row);

  /**
   * Adaptation of this class for {@link Predicate} support.  Uses the
   * {@link #matches} method.
   */
  @Override
  public boolean test(Row row) {
    return matches(row);
  }

  /**
   * Returns an iterable which filters the given iterable based on this
   * filter.
   *
   * @param iterable row iterable to filter
   *
   * @return a filtering iterable
   */
  public Iterable<Row> apply(Iterable<? extends Row> iterable)
  {
    return new FilterIterable(iterable);
  }

  /**
   * Convenience method to apply this filter to the given iterable and return
   * it as a Stream.
   */
  public Stream<Row> filter(Iterable<? extends Row> iterable) {
    return StreamSupport.stream(
        new FilterIterable(iterable).spliterator(), false)
      .filter(this);
  }

  /**
   * Creates a filter based on a row pattern.
   *
   * @param rowPattern Map from column names to the values to be matched.
   *                   A table row will match the target if
   *                   {@code Objects.equals(rowPattern.get(s), row.get(s))}
   *                   for all column names in the pattern map.
   * @return a filter which matches table rows which match the values in the
   *         row pattern
   */
  public static RowFilter matchPattern(final Map<String,?> rowPattern)
  {
    return new RowFilter() {
        @Override
        public boolean matches(Row row)
        {
          for(Map.Entry<String,?> e : rowPattern.entrySet()) {
            if(!Objects.equals(e.getValue(), row.get(e.getKey()))) {
              return false;
            }
          }
          return true;
        }
      };
  }

  /**
   * Creates a filter based on a single value row pattern.
   *
   * @param columnPattern column to be matched
   * @param valuePattern value to be matched.
   *                     A table row will match the target if
   *                     {@code Objects.equals(valuePattern, row.get(columnPattern.getName()))}.
   * @return a filter which matches table rows which match the value in the
   *         row pattern
   */
  public static RowFilter matchPattern(final Column columnPattern,
                                       final Object valuePattern)
  {
    return new RowFilter() {
        @Override
        public boolean matches(Row row)
        {
          return Objects.equals(valuePattern, columnPattern.getRowValue(row));
        }
      };
  }

  /**
   * Creates a filter which inverts the sense of the given filter (rows which
   * are matched by the given filter will not be matched by the returned
   * filter, and vice versa).
   *
   * @param filter filter which to invert
   *
   * @return a RowFilter which matches rows not matched by the given filter
   */
  public static RowFilter invert(final RowFilter filter)
  {
    return new RowFilter() {
        @Override
        public boolean matches(Row row)
        {
          return !filter.matches(row);
        }
      };
  }


  /**
   * Returns an iterable which filters the given iterable based on the given
   * rowFilter.
   *
   * @param rowFilter the filter criteria, may be {@code null}
   * @param iterable row iterable to filter
   *
   * @return a filtering iterable (or the given iterable if a {@code null}
   *         filter was given)
   */
  @SuppressWarnings("unchecked")
  public static Iterable<Row> apply(RowFilter rowFilter,
                                    Iterable<? extends Row> iterable)
  {
    return((rowFilter != null) ? rowFilter.apply(iterable) :
           (Iterable<Row>)iterable);
  }


  /**
   * Iterable which creates a filtered view of a another row iterable.
   */
  private class FilterIterable implements Iterable<Row>
  {
    private final Iterable<? extends Row> _iterable;

    private FilterIterable(Iterable<? extends Row> iterable)
    {
      _iterable = iterable;
    }


    /**
     * Returns an iterator which iterates through the rows of the underlying
     * iterable, returning only rows for which the {@link RowFilter#matches}
     * method returns {@code true}
     */
    @Override
    public Iterator<Row> iterator()
    {
      return new Iterator<Row>() {
        private final Iterator<? extends Row> _iter = _iterable.iterator();
        private Row _next;

        @Override
        public boolean hasNext() {
          while(_iter.hasNext()) {
            _next = _iter.next();
            if(RowFilter.this.matches(_next)) {
              return true;
            }
          }
          _next = null;
          return false;
        }

        @Override
        public Row next() {
          if(_next == null) {
            throw new NoSuchElementException();
          }
          return _next;
        }

      };
    }

  }

}
