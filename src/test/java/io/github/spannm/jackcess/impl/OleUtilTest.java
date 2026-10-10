/*
 * Copyright (c) 2026 Markus Spann
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
package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.BLOB;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import io.github.spannm.jackcess.util.OleBlob;
import org.junit.jupiter.params.ParameterizedTest;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Tests that OLE data can be read without the optional POI library on the classpath.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class OleUtilTest extends AbstractBaseTest {

    /** a blob of the test database which holds compound storage data */
    private static final String COMPOUND_ROW = "test_word.doc";

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(BLOB)
    void parseBlob_withoutPoi_readsCompoundStorageAsOther(TestDb testDb) throws Exception {
        byte[] bytes = readOleData(testDb, COMPOUND_ROW);
        assertThat(OleUtil.parseBlob(bytes).getContent().getType()).isEqualTo(OleBlob.ContentType.COMPOUND_STORAGE);

        try (URLClassLoader loader = newLoaderWithoutPoi()) {
            assertThatThrownBy(() -> loader.loadClass("org.apache.poi.poifs.filesystem.POIFSFileSystem"))
                .isInstanceOf(ClassNotFoundException.class);

            Class<?> oleUtil = Class.forName(OleUtil.class.getName(), true, loader);
            Class<?> oleBlob = loader.loadClass(OleBlob.class.getName());
            Class<?> content = loader.loadClass(OleBlob.Content.class.getName());
            Class<?> otherContent = loader.loadClass(OleBlob.OtherContent.class.getName());

            Object blob = oleUtil.getMethod("parseBlob", byte[].class).invoke(null, (Object) bytes);
            Object blobContent = oleBlob.getMethod("getContent").invoke(blob);

            assertThat(content.getMethod("getType").invoke(blobContent)).hasToString(OleBlob.ContentType.OTHER.name());
            assertThat(otherContent.isInstance(blobContent)).isTrue();
        }
    }

    /**
     * Reads the raw OLE data of the named row of the blob test table.
     *
     * @param testDb  test database to read
     * @param rowName value of the name column of the row
     * @return the raw OLE data
     */
    private static byte[] readOleData(TestDb testDb, String rowName) throws Exception {
        try (Database db = testDb.open()) {
            for (Row row : db.getTable("Table1")) {
                if (rowName.equals(row.getString("name"))) {
                    return row.getBytes("ole_data");
                }
            }
        }
        throw new AssertionError(String.format("No row %s in %s", rowName, testDb));
    }

    /**
     * Creates a class loader which loads the Jackcess classes, but not POI. Its parent is the platform class loader,
     * which does not see the test classpath.
     *
     * @return the class loader
     */
    private static URLClassLoader newLoaderWithoutPoi() {
        URL jackcessClasses = OleUtil.class.getProtectionDomain().getCodeSource().getLocation();
        return new URLClassLoader(new URL[] {jackcessClasses}, ClassLoader.getSystemClassLoader().getParent());
    }
}
