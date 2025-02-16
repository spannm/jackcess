package io.github.spannm.jackcess.expr;

/**
 * Base class for exceptions thrown during expression evaluation.
 */
public class EvalException extends IllegalStateException {
    private static final long serialVersionUID = 20180330L;

    public EvalException(String message) {
        super(message);
    }

    public EvalException(Throwable cause) {
        super(cause);
    }

    public EvalException(String message, Throwable cause) {
        super(message, cause);
    }
}
