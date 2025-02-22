/*
Copyright (c) 2014 James Ahlborn

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

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Column;

/**
 * Factory which generates appropriate ColumnValidators when Column instances are created.
 */
@FunctionalInterface
public interface ColumnValidatorFactory {
    /**
     * Returns a ColumnValidator instance for the given column, or {@code null} if the default should be used.
     */
    ColumnValidator createValidator(Column col);
}
