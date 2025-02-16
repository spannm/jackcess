package io.github.spannm.jackcess.expr;

import javax.script.Bindings;

/**
 * EvalContext encapsulates all shared state for expression parsing and evaluation. It provides a bridge between the
 * expression execution engine and the current Database.
 */
public interface EvalContext extends LocaleContext {
    /**
     * @param seed the seed for the random value, following the rules for the "Rnd" function
     * @return a random value for the given seed following the statefulness rules for the "Rnd" function
     */
    float getRandom(Integer seed);

    /**
     * @return the expected type of the result value for the current expression evaluation (for "default value" and
     *         "calculated" expressions)
     */
    Value.Type getResultType();

    /**
     * @return the value of the "current" column (for "field validator" expressions)
     */
    Value getThisColumnValue();

    /**
     * @return the value of the entity identified by the given identifier (for "calculated" and "row validator"
     *         expressions)
     */
    Value getIdentifierValue(Identifier identifier);

    /**
     * @return the currently configured Bindings (from the {@link EvalConfig})
     */
    Bindings getBindings();

    /**
     * @return the value of the current key from the currently configured {@link Bindings}
     */
    Object get(String key);

    /**
     * Sets the value of the given key to the given value in the currently configured {@link Bindings}.
     */
    void put(String key, Object value);
}
