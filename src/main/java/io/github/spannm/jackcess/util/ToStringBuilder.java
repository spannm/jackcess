package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.impl.ByteUtil;
import io.github.spannm.jackcess.impl.PageChannel;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Builder for {@link Object#toString()} methods.
 */
public class ToStringBuilder {
    /** Object registry for avoidance of cycles. */
    private static final Map<Object, Object> OBJ_REGISTRY        = new WeakHashMap<>();

    private final StringBuilder              buffer;
    private final Object                     object;

    private final String                     fieldSeparator;
    private final boolean                    fieldSeparatorAtStart;
    private final String                     fieldNameValueSeparator;
    private final String                     contentStart;
    private final String                     contentEnd;
    private final String                     nullText;
    private final String                     implSuffix;
    private boolean                          useIdentityHashCode = true;
    private final int                        maxByteDetailLen    = 20;

    ToStringBuilder(Object _object, String _fieldSeparator, boolean _fieldSeparatorAtStart, String _fieldNameValueSeparator, String _contentEnd, boolean _useIdentityHashCode) {
        buffer = new StringBuilder(512);
        object = _object;
        fieldSeparator = Optional.ofNullable(_fieldSeparator).orElse(",");
        fieldSeparatorAtStart = _fieldSeparatorAtStart;
        fieldNameValueSeparator = Optional.ofNullable(_fieldNameValueSeparator).orElse("=");
        contentStart = "[";
        contentEnd = Optional.ofNullable(_contentEnd).orElse("]");
        nullText = "<null>";
        implSuffix = "Impl";
        useIdentityHashCode = _useIdentityHashCode;

        if (object != null) {
            buffer.append(_object instanceof String ? _object : getShortClassName(_object.getClass(), implSuffix));
            if (useIdentityHashCode) {
                buffer.append('@').append(Integer.toHexString(System.identityHashCode(object)));
            }

            buffer.append(contentStart);
            if (fieldSeparatorAtStart) {
                buffer.append(fieldSeparator);
            }
        }
    }

    public static ToStringBuilder valueBuilder(Object obj) {
        return new ToStringBuilder(obj, null, false, null, null, false);
    }

    public static ToStringBuilder builder(Object obj) {
        return new ToStringBuilder(obj, System.lineSeparator() + "  ", true, ": ", System.lineSeparator() + "]", true);
    }

    public ToStringBuilder append(String fieldName, Object value) {
        if (fieldName != null) {
            buffer.append(fieldName).append(fieldNameValueSeparator);
        }

        if (value == null) {
            buffer.append(nullText);
        } else {
            appendInternal(buffer, fieldName, value);
        }

        buffer.append(fieldSeparator);
        return this;
    }

    public ToStringBuilder appendIgnoreNull(String fieldName, Object value) {
        return append(fieldName, value == null ? "" : value);
    }

    @Override
    public String toString() {
        if (object == null) {
            buffer.append(nullText);
        } else {
            removeLastFieldSeparator(buffer, fieldSeparator);
            buffer.append(contentEnd);
        }
        return buffer.toString();
    }

    void appendInternal(StringBuilder _buffer, String _fieldName, Object _value) {
        boolean primitiveWrapper = _value instanceof Number || _value instanceof Boolean || _value instanceof Character;
        if (OBJ_REGISTRY.containsKey(_value) && !primitiveWrapper) {
            _buffer.append(_value.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(_value)));
            return;
        }

        OBJ_REGISTRY.put(_value, null); // register object

        try {
            if (_value instanceof byte[]) {
                ByteBuffer bb = PageChannel.wrap((byte[]) _value);
                int len = bb.remaining();
                _buffer.append('(').append(len).append(") ").append(ByteUtil.toHexString(bb, bb.position(), Math.min(len, maxByteDetailLen)));
                if (len > maxByteDetailLen) {
                    _buffer.append("...");
                }
            } else if (_value.getClass().isArray()) {
                _buffer.append('{');
                for (int i = 0; i < Array.getLength(_value); i++) {
                    Object item = Array.get(_value, i);
                    if (i > 0) {
                        _buffer.append(fieldSeparator);
                    }
                    if (item == null) {
                        _buffer.append(nullText);
                    } else {
                        appendInternal(_buffer, _fieldName, item); // recursive call
                    }
                }
                _buffer.append('}');
            } else if (_value instanceof Collection<?>) {
                String str = ((Collection<?>) _value).stream().map(v -> v == null ? nullText : v.toString()).collect(Collectors.joining(","));
                _buffer.append('[').append(str).append(']');
            } else if (_value instanceof Map<?, ?>) {
                String str = ((Map<?, ?>) _value).entrySet().stream().map(e -> e.getKey() + "=" + (e.getValue() == null ? nullText : e.getValue())).collect(Collectors.joining(","));
                _buffer.append('{').append(str).append('}');
            } else {
                _buffer.append(_value);
            }
        } finally {
            OBJ_REGISTRY.remove(_value); // unregister object
        }
    }

    static String getShortClassName(Class<?> clazz, String _implSuffix) {
        String nm = clazz.getSimpleName();
        if (nm.endsWith(_implSuffix)) {
            nm = nm.substring(0, nm.length() - _implSuffix.length());
        }
        int idx = nm.lastIndexOf('.');
        return idx >= 0 ? nm.substring(idx + 1) : nm;
    }

    static void removeLastFieldSeparator(StringBuilder _buffer, String _fieldSeparator) {
        int len = _buffer.length();
        int sepLen = _fieldSeparator.length();
        if (len > 0 && sepLen > 0 && len >= sepLen) {
            boolean match = true;
            for (int i = 0; i < sepLen; i++) {
                if (_buffer.charAt(len - 1 - i) != _fieldSeparator.charAt(sepLen - 1 - i)) {
                    return;
                }
            }
            if (match) {
                _buffer.setLength(len - sepLen);
            }
        }
    }

}
