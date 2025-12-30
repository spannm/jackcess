/*
 * Copyright (C) 2024- Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.JetFormat;

import java.io.File;
import java.io.IOException;
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

    public Database open() throws IOException {
        return TestUtil.openDb(getExpectedFileFormat(), getFile(), false, getExpectedCharset());
    }

    public Database openMem() throws IOException {
        return TestUtil.openDb(getExpectedFileFormat(), getFile(), true, getExpectedCharset(), false);
    }

    public Database openCopy() throws IOException {
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
