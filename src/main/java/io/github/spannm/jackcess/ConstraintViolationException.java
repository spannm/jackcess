package io.github.spannm.jackcess;

/**
 * JackcessException which indicates that the failure was caused by a database constraint violation.
 *
 * @author James Ahlborn
 */
public class ConstraintViolationException extends JackcessException {
    private static final long serialVersionUID = 20131123L;

    public ConstraintViolationException(String msg) {
        super(msg);
    }
}
