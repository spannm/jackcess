package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
}
