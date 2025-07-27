package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.test.source.DbsInPathSource.DbsInPathSourceArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.support.AnnotationSupport;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code @DbsInPathSource} is an {@link ArgumentsSource} that provides test databases found in a path to test cases.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(DbsInPathSourceArgumentsProvider.class)
public @interface DbsInPathSource {

    /**
     * Path to search databases in.<br>
     * The annotation is deliberately named {@code value}, so the parameter name can be left out.
     */
    String value();

    class DbsInPathSourceArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ParameterDeclarations _parameters, ExtensionContext _context) throws IOException {
            DbsInPathSource src = _context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, DbsInPathSource.class).get()).orElse(null);
            Path path = Paths.get(src.value());
            try (Stream<Path> walk = Files.walk(path)) {

                List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .filter(p -> {
                        String nm = p.toString().toLowerCase();
                        return nm.endsWith(".mdb") || nm.endsWith(".accdb");
                    })
                    .sorted()
                    .collect(Collectors.toList());
                return files.stream().map(Arguments::of);
            }
        }

    }

}
