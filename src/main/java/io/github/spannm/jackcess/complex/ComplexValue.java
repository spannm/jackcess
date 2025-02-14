package io.github.spannm.jackcess.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.RowId;
import io.github.spannm.jackcess.impl.complex.ComplexColumnInfoImpl;

import java.io.IOException;
import java.io.ObjectStreamException;

/**
 * Base interface for a value in a complex column (where there may be multiple values for a single row in the main
 * table).
 *
 * @author James Ahlborn
 */
public interface ComplexValue {
    /**
     * Returns the unique identifier of this complex value (this value is unique among all values in all rows of the
     * main table).
     *
     * @return the current id or {@link ComplexColumnInfoImpl#INVALID_ID} for a new, unsaved value.
     */
    Id getId();

    /**
     * Called once when a new ComplexValue is saved to set the new unique identifier.
     */
    void setId(Id newId);

    /**
     * Returns the foreign key identifier for this complex value (this value is the same for all values in the same row
     * of the main table).
     *
     * @return the current id or {@link ComplexColumnInfoImpl#INVALID_FK} for a new, unsaved value.
     */
    ComplexValueForeignKey getComplexValueForeignKey();

    void setComplexValueForeignKey(ComplexValueForeignKey complexValueFk);

    /**
     * @return the column in the main table with which this complex value is associated
     */
    Column getColumn();

    /**
     * Writes any updated data for this complex value to the database.
     */
    void update() throws IOException;

    /**
     * Deletes the data for this complex value from the database.
     */
    void delete() throws IOException;

    /**
     * Identifier for a ComplexValue. Only valid for comparing complex values for the same column.
     */
    abstract class Id extends Number {
        private static final long serialVersionUID = 20130318L;

        @Override
        public byte byteValue() {
            return (byte) get();
        }

        @Override
        public short shortValue() {
            return (short) get();
        }

        @Override
        public int intValue() {
            return get();
        }

        @Override
        public long longValue() {
            return get();
        }

        @Override
        public float floatValue() {
            return get();
        }

        @Override
        public double doubleValue() {
            return get();
        }

        @Override
        public int hashCode() {
            return get();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && get() == ((Id) o).get();
        }

        @Override
        public String toString() {
            return String.valueOf(get());
        }

        protected final Object writeReplace() throws ObjectStreamException {
            // if we are going to serialize this ComplexValue.Id, convert it back to
            // a normal Integer (in case it is restored outside of the context of jackcess)
            return get();
        }

        /**
         * Returns the unique identifier of this complex value (this value is unique among all values in all rows of the
         * main table for the complex column).
         */
        public abstract int get();

        /**
         * Returns the rowId of this ComplexValue within the secondary table.
         */
        public abstract RowId getRowId();
    }
}
