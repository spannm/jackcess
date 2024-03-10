package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.JetFormat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Defines known valid test database files and their jet format version.
 */
public final class TestDB {

    /** Charset for access 97 databases. */
    private static final Charset                                ACC97_CHARSET      = Charset.forName("windows-1252");

    private static final Map<Basename, Map<FileFormat, TestDB>> TESTDBS_MAP        = buildMapOfTestDbs();

    /**
     * Defines currently supported database file formats that are neither read-only nor {@value FileFormat#MSISAM} (MS Money).
     */
    private static final FileFormat[]                           FILE_FORMATS_WRITE = Arrays.stream(FileFormat.values())
        .filter(ff -> !DatabaseImpl.getFileFormatDetails(ff).getFormat().READ_ONLY && ff != FileFormat.MSISAM)
        .toArray(FileFormat[]::new);

    private final File       databaseFile;
    private final FileFormat expectedFileFormat;
    private final Charset    charset;

    private TestDB(File _databaseFile, FileFormat _expectedFileFormat, Charset _charset) {
        databaseFile = _databaseFile;
        expectedFileFormat = _expectedFileFormat;
        charset = _charset;
    }

    public File getFile() {
        return databaseFile;
    }

    public FileFormat getExpectedFileFormat() {
        return expectedFileFormat;
    }

    public JetFormat getExpectedJetFormat() {
        return DatabaseImpl.getFileFormatDetails(expectedFileFormat).getFormat();
    }

    public Charset getExpectedCharset() {
        return charset;
    }

    @Override
    public String toString() {
        return String.format("%s[dbFile=%s, fileFormat=%s, charset=%s]",
            getClass().getSimpleName(), databaseFile, expectedFileFormat, charset);
    }

    public Database open() throws Exception {
        return TestUtil.open(getExpectedFileFormat(), getFile(), false, getExpectedCharset());
    }

    public Database openMem() throws Exception {
        return TestUtil.openDB(getExpectedFileFormat(), getFile(), true, getExpectedCharset(), false);
    }

    public Database openCopy() throws Exception {
        return TestUtil.openCopy(getExpectedFileFormat(), getFile(), false);
    }

    private static Map<Basename, Map<FileFormat, TestDB>> buildMapOfTestDbs() {
        Map<Basename, Map<FileFormat, TestDB>> map = new EnumMap<>(Basename.class);
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

                TestDB testDb = new TestDB(testFile, fileFormat, charset);
                map.computeIfAbsent(basename, k -> new EnumMap<>(FileFormat.class)).put(fileFormat, testDb);
            }
        }
        return map;
    }

    public static FileFormat[] getSupportedFileformats() {
        return FILE_FORMATS_WRITE;
    }

    public static List<TestDB> getSupportedTestDbs(Basename _basename) {
        return Arrays.stream(FILE_FORMATS_WRITE)
            .map(ff -> TESTDBS_MAP.get(_basename).get(ff))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<TestDB> getSupportedTestDbsReadOnly(Basename _basename) {
        return Arrays.stream(FileFormat.values())
            .map(ff -> TESTDBS_MAP.get(_basename).get(ff))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<TestDB> getSupportedTestDbs() {
        return getSupportedTestDbs(Basename.TEST);
    }

    public static List<TestDB> getSupportedTestDbsReadOnly() {
        return getSupportedTestDbsReadOnly(Basename.TEST);
    }

}
