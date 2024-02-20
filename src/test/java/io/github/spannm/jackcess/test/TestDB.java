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
import java.util.ArrayList;
import java.util.List;

/**
 * Defines known valid test database files and their jet format version.
 */
public final class TestDB {

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

    public static List<TestDB> getSupportedTestDbs(Basename basename) {
        return getSupportedTestDbs(basename, false);
    }

    public static List<TestDB> getSupportedTestDbsForRead(Basename basename) {
        return getSupportedTestDbs(basename, true);
    }

    static List<TestDB> getSupportedTestDbs(Basename basename, boolean readOnly) {

        List<TestDB> supportedTestDbs = new ArrayList<>();
        for (FileFormat fileFormat : readOnly ? FileFormat.values() : AbstractBaseTest.getSupportedFileformats()) {
            File testFile = new File(AbstractBaseTest.DIR_TEST_DATA,
                fileFormat.name() + File.separator + basename + fileFormat.name() + fileFormat.getFileExtension());
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

            Charset charset = fileFormat == FileFormat.V1997 ? AbstractBaseTest.A97_CHARSET : null;

            supportedTestDbs.add(new TestDB(testFile, fileFormat, charset));
        }
        return supportedTestDbs;
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

}
