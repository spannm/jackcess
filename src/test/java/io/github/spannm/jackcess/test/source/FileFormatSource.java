package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.source.FileFormatSource.FileFormatArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * {@code @FileFormatSource} is an {@link ArgumentsSource} that provides writable file formats to test cases.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(FileFormatArgumentsProvider.class)
public @interface FileFormatSource {

    static class FileFormatArgumentsProvider implements ArgumentsProvider {

        /**
         * Defines currently supported database file formats that are neither read-only nor {@value FileFormat#MSISAM} (MS Money).
         */
        private static final FileFormat[] FILE_FORMATS_WRITE = Arrays.stream(FileFormat.values())
            .filter(ff -> !DatabaseImpl.getFileFormatDetails(ff).getFormat().READ_ONLY && ff != FileFormat.MSISAM)
            .toArray(FileFormat[]::new);

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(FILE_FORMATS_WRITE).map(Arguments::of);
        }

    }

}
