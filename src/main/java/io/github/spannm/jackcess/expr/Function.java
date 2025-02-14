package io.github.spannm.jackcess.expr;

/**
 * A Function provides an invokable handle to external functionality to an expression.
 *
 * @author James Ahlborn
 */
public interface Function {

    /**
     * @return the name of this function
     */
    String getName();

    /**
     * Evaluates this function within the given context with the given parameters.
     *
     * @return the result of the function evaluation
     */
    Value eval(EvalContext ctx, Value... params);

    /**
     * @return {@code true} if this function is a "pure" function, {@code false} otherwise. A pure function will always
     *         return the same result for a given set of parameters and has no side effects.
     */
    boolean isPure();
}
