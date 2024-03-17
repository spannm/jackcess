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
    private final FileFormat fileFormat;
    private final Charset    charset;

    public TestDb(File _databaseFile, FileFormat _fileFormat, Charset _charset) {
        databaseFile = _databaseFile;
        fileFormat = _fileFormat;
        charset = _charset;
    }

    public File getFile() {
        return databaseFile;
    }

    public FileFormat getExpectedFileFormat() {
        return fileFormat;
    }

    public JetFormat getExpectedJetFormat() {
        return DatabaseImpl.getFileFormatDetails(fileFormat).getFormat();
    }

    public Charset getExpectedCharset() {
        return charset;
    }

    public Database open() throws Exception {
        return TestUtil.openDb(getExpectedFileFormat(), getFile(), false, getExpectedCharset());
    }

    public Database openMem() throws Exception {
        return TestUtil.openDb(getExpectedFileFormat(), getFile(), true, getExpectedCharset(), false);
    }

    public Database openCopy() throws Exception {
        return TestUtil.openCopy(getExpectedFileFormat(), getFile(), false);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + '['
            + databaseFile
            + ", fileFormat=" + fileFormat
            + (charset == null ? "" : ", charset=" + charset)
            + ']';
    }

}
