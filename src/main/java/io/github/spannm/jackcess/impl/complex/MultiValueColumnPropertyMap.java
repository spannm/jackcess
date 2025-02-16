package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.impl.PropertyMapImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * PropertyMap implementation for multi-value, complex properties. The properties for these columns seem to be dispersed
 * between both the primary column and the complex value column. The primary column only seems to have the simple
 * "multi-value" property and the rest seem to be on the complex value column. This PropertyMap implementation combines
 * them into one synthetic map.
 */
public class MultiValueColumnPropertyMap implements PropertyMap {
    /** properties from the primary column */
    private final PropertyMap primary;
    /** properties from the complex column */
    private final PropertyMap complex;

    public MultiValueColumnPropertyMap(PropertyMap _primary, PropertyMap _complex) {
        primary = _primary;
        complex = _complex;
    }

    @Override
    public String getName() {
        return primary.getName();
    }

    @Override
    public int getSize() {
        return primary.getSize() + complex.getSize();
    }

    @Override
    public boolean isEmpty() {
        return primary.isEmpty() && complex.isEmpty();
    }

    @Override
    public Property get(String name) {
        Property prop = primary.get(name);
        if (prop != null) {
            return prop;
        }
        return complex.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getValue(name, null);
    }

    @Override
    public Object getValue(String name, Object defaultValue) {
        Property prop = get(name);
        return prop != null ? prop.getValue() : defaultValue;
    }

    @Override
    public Property put(String name, Object value) {
        return put(name, null, value, false);
    }

    @Override
    public Property put(String name, DataType type, Object value) {
        return put(name, type, value, false);
    }

    @Override
    public Property put(String name, DataType type, Object value, boolean isDdl) {
        // the only property which seems to go in the "primary" is the "multi
        // value" property
        if (isPrimaryKey(name)) {
            return primary.put(name, DataType.BOOLEAN, value, true);
        }
        return complex.put(name, type, value, isDdl);
    }

    @Override
    public void putAll(Iterable<? extends Property> props) {
        if (props == null) {
            return;
        }

        for (Property prop : props) {
            if (isPrimaryKey(prop.getName())) {
                ((PropertyMapImpl) primary).put(prop);
            } else {
                ((PropertyMapImpl) complex).put(prop);
            }
        }
    }

    @Override
    public Property remove(String name) {
        if (isPrimaryKey(name)) {
            return primary.remove(name);
        }
        return complex.remove(name);
    }

    @Override
    public void save() throws IOException {
        primary.save();
        complex.save();
    }

    @Override
    public Iterator<Property> iterator() {
        final List<Iterator<Property>> iters = new ArrayList<>(2);
        iters.add(primary.iterator());
        iters.add(complex.iterator());

        return new Iterator<>() {
            private Iterator<Property> _cur;
            private Property           _next = findNext();

            private Property findNext() {
                while (!iters.isEmpty()) {
                    _cur = iters.get(0);
                    if (_cur.hasNext()) {
                        return _cur.next();
                    }
                    iters.remove(0);
                    _cur = null;
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return _next != null;
            }

            @Override
            public Property next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Property prop = _next;
                _next = findNext();
                return prop;
            }

            @Override
            public void remove() {
                if (_cur != null) {
                    _cur.remove();
                    _cur = null;
                }
            }
        };
    }

    @Override
    public String toString() {
        return PropertyMapImpl.toString(this);
    }

    private static boolean isPrimaryKey(String name) {
        // the multi-value key seems to be the only one on the primary column
        return ALLOW_MULTI_VALUE_PROP.equals(name);
    }
}
