package io.github.spannm.jackcess.expr;

/**
 * Exception thrown when expression parsing fails.
 */
public class ParseException extends EvalException {
    private static final long serialVersionUID = 20180330L;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
