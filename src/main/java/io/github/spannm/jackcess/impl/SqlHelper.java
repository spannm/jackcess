package io.github.spannm.jackcess.impl;

import java.io.IOException;

/**
 * Helper class to isolate the java.sql module interactions from the core of jackcess (in java 9+ environments). If the
 * module is enabled (indicating that the application is already using sql constructs), then jackcess will seamlessly
 * interact with sql types. If the module is not enabled (indicating that the application is not using any sql
 * constructs), then jackcess will not require the module in order to function otherwise normally.
 * <p>
 * This base class is the "fallback" class if the java.sql module is not available.
 *
 * @author James Ahlborn
 */
public class SqlHelper {

    public static final SqlHelper INSTANCE = loadInstance();

    public SqlHelper() {
    }

    public boolean isBlob(Object value) {
        return false;
    }

    public byte[] getBlobBytes(Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isClob(Object value) {
        return false;
    }

    public CharSequence getClobString(Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Integer getNewSqlType(String typeName) throws Exception {
        throw new UnsupportedOperationException();
    }

    private static SqlHelper loadInstance() {
        // attempt to load the implementation of this class which works with
        // java.sql classes. if that fails, use this fallback instance instead.
        try {
            return (SqlHelper) Class.forName("io.github.spannm.jackcess.impl.SqlHelperImpl")
                .getDeclaredConstructor()
                .newInstance();
        } catch (Throwable ignored) {}
        return new SqlHelper();
    }
}
