package io.github.spannm.jackcess;

/**
 * Unspecific {@code Jackcess} run-time exception.
 */
public class JackcessRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JackcessRuntimeException(String _message) {
        this(_message, null);
    }

    public JackcessRuntimeException(Throwable _cause) {
        this(null, _cause);
    }

    public JackcessRuntimeException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

}
