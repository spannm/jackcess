package io.github.spannm.jackcess.expr;

import java.util.Objects;
import java.util.Optional;

/**
 * identifies a database entity (e.g. the name of a database field). An Identify must have an object name, but the
 * collection name and property name are optional.
 */
public class Identifier {
    private final String collectionName;
    private final String objectName;
    private final String propertyName;

    public Identifier(String _objectName) {
        this(null, _objectName, null);
    }

    public Identifier(String _collectionName, String _objectName, String _propertyName) {
        collectionName = _collectionName;
        objectName = _objectName;
        propertyName = _propertyName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public int hashCode() {
        return objectName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Identifier)) {
            return false;
        }

        Identifier oi = (Identifier) o;

        return Objects.equals(objectName, oi.objectName)
            && Objects.equals(collectionName, oi.collectionName)
            && Objects.equals(propertyName, oi.propertyName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Optional.ofNullable(collectionName).ifPresent(x -> sb.append('[').append(x).append("]."));
        sb.append('[').append(objectName).append(']');
        Optional.ofNullable(propertyName).ifPresent(x -> sb.append('[').append(x).append("]."));
        return sb.toString();
    }

}
