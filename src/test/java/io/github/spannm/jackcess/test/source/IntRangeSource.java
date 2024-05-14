package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.test.source.IntRangeSource.IntRangeArgumentsProvider;
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
 * {@code @IntRangeSource} is an {@link ArgumentsSource} that provides consecutive
 * integer ranges configured via the {@link #start} and {@link #end} attributes.<p>
 * {@code start} is always inclusive, while {@code end} is exclusive by default,
 * however it can be set to {@code inclusive} using attribute {@code endInclusive}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(IntRangeArgumentsProvider.class)
public @interface IntRangeSource {

    /**
     * The start value of the range.
     */
    int start();

    /**
     * The end value of the range.
     */
    int end();

    /**
     * Whether the end value is inclusive (to be included in the range), default {@code false}.
     */
    boolean endInclusive() default false;

    class IntRangeArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            Optional<IntRangeSource> optSrc = context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, IntRangeSource.class).get());
            return optSrc.map(cfg -> IntStream.range(cfg.start(), cfg.endInclusive() ? cfg.end() + 1 : cfg.end()).boxed().map(Arguments::of)).orElse(Stream.empty());
        }

    }

}
