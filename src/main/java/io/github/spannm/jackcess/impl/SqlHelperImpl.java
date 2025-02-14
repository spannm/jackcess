package io.github.spannm.jackcess.impl;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Implementation of SqlHelperImpl which works with the java.sql modules classes. This class is used if the java.sql
 * module is enabled in the application.
 *
 * @author James Ahlborn
 */
public class SqlHelperImpl extends SqlHelper {

    public SqlHelperImpl() {
    }

    @Override
    public boolean isBlob(Object value) {
        return value instanceof Blob;
    }

    @Override
    public byte[] getBlobBytes(Object value) throws IOException {
        try {
            Blob b = (Blob) value;
            // note, start pos is 1-based
            return b.getBytes(1L, (int) b.length());
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isClob(Object value) {
        return value instanceof Clob;
    }

    @Override
    public CharSequence getClobString(Object value) throws IOException {
        try {
            Clob c = (Clob) value;
            // note, start pos is 1-based
            return c.getSubString(1L, (int) c.length());
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Integer getNewSqlType(String typeName) throws Exception {
        java.lang.reflect.Field sqlTypeField = Types.class.getField(typeName);
        return (Integer) sqlTypeField.get(null);
    }
}
