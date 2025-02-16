package io.github.spannm.jackcess.complex;

/**
 * Complex value corresponding to an single value in a multi-value column.
 */
public interface SingleValue extends ComplexValue {
    Object get();

    void set(Object value);
}
