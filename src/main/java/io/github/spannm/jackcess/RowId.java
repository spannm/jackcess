package io.github.spannm.jackcess;

/**
 * Uniquely identifies a row of data within the access database. While RowIds are largely opaque identifiers, they are
 * comparable to each other (within the same table) and have valid {@code equals()}, {@code hashCode()} and
 * {@code toString()} methods.
 *
 * @author James Ahlborn
 */
public interface RowId extends Comparable<RowId> {

}
