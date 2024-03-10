package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.JetFormat;

import java.io.File;
import java.nio.charset.Charset;

/**
 * A valid test database file and its jet format version.
 */
public final class TestDb {

    private final File       databaseFile;
    private final FileFormat expectedFileFormat;
    private final Charset    charset;

    public TestDb(File _databaseFile, FileFormat _expectedFileFormat, Charset _charset) {
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

    public Database open() throws Exception {
        return TestUtil.open(getExpectedFileFormat(), getFile(), false, getExpectedCharset());
    }

    public Database openMem() throws Exception {
        return TestUtil.openDB(getExpectedFileFormat(), getFile(), true, getExpectedCharset(), false);
    }

    public Database openCopy() throws Exception {
        return TestUtil.openCopy(getExpectedFileFormat(), getFile(), false);
    }

    @Override
    public String toString() {
        return String.format("%s[dbFile=%s, fileFormat=%s, charset=%s]",
            getClass().getSimpleName(), databaseFile, expectedFileFormat, charset);
    }

}
