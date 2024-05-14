package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.test.source.IntMatrixSource.IntMatrixArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@code @IntMatrixSource} is an {@link ArgumentsSource} that provides
 * all combinations of two consecutive integer ranges configured
 * via the {@link #start} and {@link #end} attributes.<p>
 * {@code start} is always inclusive, while {@code end} is exclusive by default,
 * however it can be set to {@code inclusive} using attribute {@code endInclusive}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(IntMatrixArgumentsProvider.class)
public @interface IntMatrixSource {

    /**
     * The start value of the matrix.
     */
    int start();

    /**
     * The end value of the matrix.
     */
    int end();

    /**
     * Whether the end value is inclusive (to be included in building the matrix), default {@code false}.
     */
    boolean endInclusive() default false;

    class IntMatrixArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            Optional<IntMatrixSource> optSrc = context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, IntMatrixSource.class).get());
            return optSrc.map(cfg -> {
                int end = cfg.end() + (cfg.endInclusive() ? 1 : 0);
                return IntStream.range(cfg.start(), end).boxed()
                    .flatMap(x -> IntStream.range(cfg.start(), end).mapToObj(y -> Arguments.of(x, y)));
            }).orElse(Stream.empty());
        }

    }

}
