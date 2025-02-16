package io.github.spannm.jackcess;

/**
 * JackcessException which is thrown from multi-add-row {@link Table} methods which indicates how many rows were
 * successfully written before the underlying failure was encountered.
 */
public class BatchUpdateException extends JackcessException {
    private static final long serialVersionUID = 20131123L;

    private final int         _updateCount;

    public BatchUpdateException(int updateCount, String msg, Throwable cause) {
        super(msg + ": " + cause, cause);
        _updateCount = updateCount;
    }

    public int getUpdateCount() {
        return _updateCount;
    }
}
