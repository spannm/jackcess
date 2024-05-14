package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.source.FileFormatSource.FileFormatArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code @FileFormatSource} is an {@link ArgumentsSource} that provides writable file formats to test cases.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(FileFormatArgumentsProvider.class)
public @interface FileFormatSource {

    /**
     * Optional names of enum constants to include.<br>
     * If specified, the names must match existing enum constants otherwise an {@link IllegalArgumentException} is thrown.<br>
     * If not specified, all enum constants are taken into consideration.
     */
    String[] include() default {};

    /**
     * Optional names of enum constants to exclude.<br>
     * If used, the names must match existing enum constants otherwise an {@link IllegalArgumentException} is thrown.
     */
    String[] exclude() default {};

    class FileFormatArgumentsProvider implements ArgumentsProvider {

        /**
         * Defines currently supported database file formats that are neither read-only nor {@value FileFormat#MSISAM} (MS Money).
         */
        private static final List<FileFormat> FILE_FORMATS_WRITE = Arrays.stream(FileFormat.values())
            .filter(ff -> !DatabaseImpl.getFileFormatDetails(ff).getFormat().READ_ONLY && ff != FileFormat.MSISAM)
            .collect(Collectors.toList());

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext _context) {
            FileFormatSource src = _context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, FileFormatSource.class).get()).orElse(null);
            List<FileFormat> include = Arrays.stream(src.include()).map(FileFormat::valueOf).collect(Collectors.toList());
            if (include.isEmpty()) {
                include.addAll(FILE_FORMATS_WRITE);
            }
            List<FileFormat> exclude = Arrays.stream(src.exclude()).map(FileFormat::valueOf).collect(Collectors.toList());

            return include.stream()
                .filter(ff -> !exclude.contains(ff))
                .map(Arguments::of);
        }

    }

}
