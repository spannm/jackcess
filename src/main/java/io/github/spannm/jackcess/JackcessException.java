package io.github.spannm.jackcess;

import java.io.IOException;

/**
 * Base class for specific exceptions thrown by Jackcess.
 *
 * @author James Ahlborn
 */
public class JackcessException extends IOException {
    private static final long serialVersionUID = 20131123L;

    public JackcessException(String message) {
        super(message);
    }

    public JackcessException(Throwable cause) {
        super(cause);
    }

    public JackcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
