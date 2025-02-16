package io.github.spannm.jackcess.expr;

/**
 * A FunctionLookup provides a source for {@link Function} instances used during expression evaluation.
 */
public interface FunctionLookup {
    /**
     * @return the function for the given function name, or {@code null} if none exists. Note that Access function names
     *         are treated in a case insensitive manner.
     */
    Function getFunction(String name);
}
