/*
 * Copyright (C) 2024- Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource.TestDbReadOnlyArgumentsProvider;
import io.github.spannm.jackcess.test.source.TestDbSource.TestDbArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

/**
 * {@code @TestDbSource} is an {@link ArgumentsSource} that provides read-only test databases to test cases.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(TestDbReadOnlyArgumentsProvider.class)
public @interface TestDbReadOnlySource {

    /**
     * Base names of read-only test databases. All base names if left empty.<br>
     * The annotation is deliberately named {@code value}, so the parameter name can be left out.
     */
    Basename[] value() default {};

    class TestDbReadOnlyArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ParameterDeclarations _parameters, ExtensionContext _context) {
            TestDbReadOnlySource src = _context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, TestDbReadOnlySource.class).get()).orElse(null);
            return src == null ? Stream.empty() : TestDbArgumentsProvider.getDbs(src.value(), FileFormat.values()).stream().map(Arguments::of);
        }
    }

}
