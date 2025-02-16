package io.github.spannm.jackcess.complex;

import java.util.Map;

/**
 * Complex value corresponding to an unsupported complex column type.
 */
public interface UnsupportedValue extends ComplexValue {
    Map<String, Object> getValues();

    Object get(String columnName);

    void set(String columnName, Object value);
}
