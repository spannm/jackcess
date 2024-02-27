package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
}
