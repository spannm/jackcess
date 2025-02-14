package io.github.spannm.jackcess.expr;

import java.util.Objects;
import java.util.Optional;

/**
 * identifies a database entity (e.g. the name of a database field). An Identify must have an object name, but the
 * collection name and property name are optional.
 *
 * @author James Ahlborn
 */
public class Identifier {
    private final String _collectionName;
    private final String _objectName;
    private final String _propertyName;

    public Identifier(String objectName) {
        this(null, objectName, null);
    }

    public Identifier(String collectionName, String objectName, String propertyName) {
        _collectionName = collectionName;
        _objectName = objectName;
        _propertyName = propertyName;
    }

    public String getCollectionName() {
        return _collectionName;
    }

    public String getObjectName() {
        return _objectName;
    }

    public String getPropertyName() {
        return _propertyName;
    }

    @Override
    public int hashCode() {
        return _objectName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Identifier)) {
            return false;
        }

        Identifier oi = (Identifier) o;

        return Objects.equals(_objectName, oi._objectName)
            && Objects.equals(_collectionName, oi._collectionName)
            && Objects.equals(_propertyName, oi._propertyName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Optional.ofNullable(_collectionName).ifPresent(x -> sb.append('[').append(x).append("]."));
        sb.append('[').append(_objectName).append(']');
        Optional.ofNullable(_propertyName).ifPresent(x -> sb.append('[').append(x).append("]."));
        return sb.toString();
    }

}
