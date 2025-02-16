package io.github.spannm.jackcess.query;

import static io.github.spannm.jackcess.impl.query.QueryFormat.*;

import java.util.List;

/**
 * Base interface for classes which encapsulate information about an Access query. The {@link #toSQLString()} method can
 * be used to convert this object into the actual SQL string which this query data represents.
 */
public interface Query {

    enum Type {
        SELECT(SELECT_QUERY_OBJECT_FLAG, 1),
        MAKE_TABLE(MAKE_TABLE_QUERY_OBJECT_FLAG, 2),
        APPEND(APPEND_QUERY_OBJECT_FLAG, 3),
        UPDATE(UPDATE_QUERY_OBJECT_FLAG, 4),
        DELETE(DELETE_QUERY_OBJECT_FLAG, 5),
        CROSS_TAB(CROSS_TAB_QUERY_OBJECT_FLAG, 6),
        DATA_DEFINITION(DATA_DEF_QUERY_OBJECT_FLAG, 7),
        PASSTHROUGH(PASSTHROUGH_QUERY_OBJECT_FLAG, 8),
        UNION(UNION_QUERY_OBJECT_FLAG, 9),
        UNKNOWN(-1, -1);

        private final int   _objectFlag;
        private final short _value;

        Type(int objectFlag, int value) {
            _objectFlag = objectFlag;
            _value = (short) value;
        }

        public int getObjectFlag() {
            return _objectFlag;
        }

        public short getValue() {
            return _value;
        }

        public static boolean isUnknown(Type _type) {
            return _type == null || UNKNOWN == _type;
        }
    }

    /**
     * Returns the name of the query.
     */
    String getName();

    /**
     * Returns the type of the query.
     */
    Type getType();

    /**
     * Whether or not this query has been marked as hidden.
     */
    boolean isHidden();

    /**
     * Returns the unique object id of the query.
     */
    int getObjectId();

    int getObjectFlag();

    /**
     * Returns the rows from the system query table from which the query information was derived.
     */
    // public List<Row> getRows();

    List<String> getParameters();

    String getOwnerAccessType();

    /**
     * Returns the actual SQL string which this query data represents.
     */
    String toSQLString();
}
