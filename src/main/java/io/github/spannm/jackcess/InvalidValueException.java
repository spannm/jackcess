package io.github.spannm.jackcess;

/**
 * JackcessException which indicates that an invalid column value was provided in a database update.
 *
 * @author James Ahlborn
 */
public class InvalidValueException extends JackcessException {
    private static final long serialVersionUID = 20180428L;

    public InvalidValueException(String msg) {
        super(msg);
    }

    public InvalidValueException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
