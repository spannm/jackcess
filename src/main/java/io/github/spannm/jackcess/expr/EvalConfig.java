/*
Copyright (c) 2018 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.expr;

import io.github.spannm.jackcess.Database;

import javax.script.Bindings;

/**
 * The EvalConfig allows for customization of the expression evaluation context for a given {@link Database} instance.
 *
 * @see io.github.spannm.jackcess.expr expression package docs
 *
 * @author James Ahlborn
 */
public interface EvalConfig {
    /**
     * @return the currently configured TemporalConfig
     */
    TemporalConfig getTemporalConfig();

    /**
     * Sets the TemporalConfig for use when evaluating expressions. The default date/time formatting is US based, so
     * this may need to be modified when interacting with {@link Database} instances from other locales.
     */
    void setTemporalConfig(TemporalConfig temporal);

    /**
     * @return the currently configured NumericConfig
     */
    NumericConfig getNumericConfig();

    /**
     * Sets the NumericConfig for use when evaluating expressions. The default date/time formatting is US based, so this
     * may need to be modified when interacting with {@link Database} instances from other locales.
     */
    void setNumericConfig(NumericConfig numeric);

    /**
     * @return the currently configured FunctionLookup
     */
    FunctionLookup getFunctionLookup();

    /**
     * Sets the {@link Function} provider to use during expression evaluation. The Functions supported by the default
     * FunctionLookup are documented in {@link io.github.spannm.jackcess.expr}. Custom Functions can be
     * implemented and provided to the expression evaluation engine by installing a custom FunctionLookup instance
     * (which would presumably wrap and delegate to the default FunctionLookup instance for any default
     * implementations).
     */
    void setFunctionLookup(FunctionLookup lookup);

    /**
     * @return the currently configured Bindings
     */
    Bindings getBindings();

    /**
     * Allows for passing custom information into expression evaluation. Currently, none of the default implementations
     * make use of the Bindings. However, in the future, customization parameters could potentially be supported via
     * custom Bindings. Additionally, custom Function instances could be passed external information via custom
     * Bindings.
     */
    void setBindings(Bindings bindings);
}
