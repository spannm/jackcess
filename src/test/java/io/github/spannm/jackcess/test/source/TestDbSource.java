package io.github.spannm.jackcess.test.source;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource.TestDbArgumentsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.support.AnnotationSupport;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code @TestDbSource} is an {@link ArgumentsSource} that provides test databases to test cases.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(TestDbArgumentsProvider.class)
public @interface TestDbSource {

    /**
     * Optional base name of test databases.
     */
    Basename basename();

    /**
     * Read-only test databases if {@code true}.
     */
    boolean readOnly() default false;

    static class TestDbArgumentsProvider implements ArgumentsProvider {

        /** Charset for access 97 databases. */
        private static final Charset                                ACC97_CHARSET      = Charset.forName("windows-1252");

        /**
         * Defines currently supported database file formats that are neither read-only nor {@value FileFormat#MSISAM} (MS Money).
         */
        private static final FileFormat[]                           FILE_FORMATS_WRITE = Arrays.stream(FileFormat.values())
            .filter(ff -> !DatabaseImpl.getFileFormatDetails(ff).getFormat().READ_ONLY && ff != FileFormat.MSISAM)
            .toArray(FileFormat[]::new);

        private static final Map<Basename, Map<FileFormat, TestDb>> TESTDBS_MAP        = buildMapOfTestDbs();

        private static Map<Basename, Map<FileFormat, TestDb>> buildMapOfTestDbs() {
            Map<Basename, Map<FileFormat, TestDb>> map = new EnumMap<>(Basename.class);
            for (Basename basename : Basename.values()) {
                for (FileFormat fileFormat : FileFormat.values()) {
                    File testDir = new File(AbstractBaseTest.DIR_TEST_DATA, fileFormat.name());
                    File testFile = new File(testDir, basename + fileFormat.name() + fileFormat.getFileExtension());

                    if (!testFile.exists()) {
                        continue;
                    }

                    // verify that the db has the expected file format
                    try (Database db = new DatabaseBuilder(testFile).withReadOnly(true).open()) {
                        FileFormat dbFileFormat = db.getFileFormat();
                        if (dbFileFormat != fileFormat) {
                            throw new RuntimeException("Expected " + fileFormat + " was " + dbFileFormat);
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }

                    Charset charset = fileFormat == FileFormat.V1997 ? ACC97_CHARSET : null;

                    TestDb testDb = new TestDb(testFile, fileFormat, charset);
                    map.computeIfAbsent(basename, k -> new EnumMap<>(FileFormat.class)).put(fileFormat, testDb);
                }
            }
            return map;
        }

        private static List<TestDb> getDbs(Basename _basename) {
            return Arrays.stream(FILE_FORMATS_WRITE)
                .map(ff -> TESTDBS_MAP.get(_basename).get(ff))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        private static List<TestDb> getReadOnlyDbs(Basename _basename) {
            return Arrays.stream(FileFormat.values())
                .map(ff -> TESTDBS_MAP.get(_basename).get(ff))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            Optional<TestDbSource> optSrc = context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, TestDbSource.class).get());
            if (optSrc.isEmpty()) {
                return Stream.empty();
            }
            TestDbSource src = optSrc.get();
            Basename basename = Objects.requireNonNull(src.basename());
            List<TestDb> dbs = src.readOnly() ? getReadOnlyDbs(basename) : getDbs(basename);
            return dbs.stream().map(Arguments::of);
        }

    }

}
