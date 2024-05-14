package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource.ReadOnlyTestDbArgumentsProvider;
import io.github.spannm.jackcess.test.source.TestDbSource.TestDbArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
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
@ArgumentsSource(ReadOnlyTestDbArgumentsProvider.class)
public @interface TestDbReadOnlySource {

    /**
     * Base name of test databases.<br>
     * The annotation is deliberately named {@code value}, so the parameter name can be left out.
     */
    Basename value();

    class ReadOnlyTestDbArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ExtensionContext _context) {
            TestDbReadOnlySource src = _context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, TestDbReadOnlySource.class).get()).orElse(null);
            return src == null ? Stream.empty() : TestDbArgumentsProvider.getDbs(src.value(), FileFormat.values()).stream().map(Arguments::of);
        }
    }

}
