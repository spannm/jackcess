/*
 * Copyright (c) 2025 Markus Spann
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
package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLFeatureNotSupportedException;

/**
 * Tests for {@link OleBlob.Builder} and the {@code OleBlob} implementation.
 */
class OleBlobBuilderTest extends AbstractBaseTest {

    private static final byte[] DATA = "some ole content".getBytes(StandardCharsets.US_ASCII);

    @Test
    void testBuilderSimplePackageVariants() throws IOException {
        OleBlob.Builder b = new OleBlob.Builder().withSimplePackageBytes(DATA);
        assertEquals(OleBlob.ContentType.SIMPLE_PACKAGE, b.getType());
        assertArrayEquals(DATA, b.getBytes());
        assertEquals(DATA.length, b.getContentLength());
        assertEquals(OleBlob.Builder.PACKAGE_PRETTY_NAME, b.getPrettyName());
        assertEquals(OleBlob.Builder.PACKAGE_TYPE_NAME, b.getClassName());

        b.withSimplePackageFileName("myfile.txt").withSimplePackageFilePath("/tmp/myfile.txt");
        assertEquals("myfile.txt", b.getFileName());
        assertEquals("/tmp/myfile.txt", b.getFilePath());

        try (OleBlob blob = b.toBlob()) {
            assertEquals(OleBlob.ContentType.SIMPLE_PACKAGE, blob.getContent().getType());
            assertNotNull(blob.toString());
        }
    }

    @Test
    void testBuilderLinkVariants() throws IOException {
        OleBlob.Builder b = new OleBlob.Builder()
            .withLinkFileName("linked.txt")
            .withLinkPath("/tmp/linked.txt");
        assertEquals(OleBlob.ContentType.LINK, b.getType());
        assertEquals("linked.txt", b.getFileName());
        assertEquals("/tmp/linked.txt", b.getFilePath());

        try (OleBlob blob = b.toBlob()) {
            OleBlob.LinkContent lc = (OleBlob.LinkContent) blob.getContent();
            assertEquals("/tmp/linked.txt", lc.getLinkPath());
            assertNotNull(lc.toString());
        }
    }

    @Test
    void testBuilderOtherStream() throws IOException {
        OleBlob.Builder b = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOtherStream(new ByteArrayInputStream(DATA), DATA.length);
        assertEquals(OleBlob.ContentType.OTHER, b.getType());
        assertNotNull(b.getStream());

        try (OleBlob blob = b.toBlob()) {
            OleBlob.OtherContent oc = (OleBlob.OtherContent) blob.getContent();
            assertEquals(DATA.length, oc.length());
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            oc.writeTo(bout);
            assertArrayEquals(DATA, bout.toByteArray());
            assertNotNull(oc.toString());
        }
    }

    @Test
    void testBuilderFromFiles() throws IOException {
        File sampleFile = new File(DIR_TEST_DATA, "sample-input.tab");
        try (OleBlob blob = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOther(sampleFile).toBlob()) {
            assertEquals(OleBlob.ContentType.OTHER, blob.getContent().getType());
        }

        OleBlob.Builder lb = new OleBlob.Builder().withLink(sampleFile);
        assertEquals(sampleFile.getName(), lb.getFileName());
        try (OleBlob blob = lb.toBlob()) {
            OleBlob.LinkContent lc = (OleBlob.LinkContent) blob.getContent();
            try (InputStream in = lc.getLinkStream()) {
                assertTrue(in.read() >= 0);
            }
        }
    }

    @Test
    void testUnsupportedContentType() {
        OleBlob.Builder b = new OleBlob.Builder();
        assertThrows(IllegalArgumentException.class, b::toBlob);
    }

    @Test
    void testBlobSqlMethods() throws Exception {
        try (OleBlob blob = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOtherBytes(DATA).toBlob()) {
            long len = blob.length();
            assertTrue(len > DATA.length);

            byte[] all = blob.getBytes(1L, (int) len);
            assertEquals(len, all.length);

            byte[] part = blob.getBytes(2L, 4);
            assertEquals(4, part.length);

            try (InputStream in = blob.getBinaryStream()) {
                assertEquals(all[0], (byte) in.read());
            }
            try (InputStream in = blob.getBinaryStream(1L, len)) {
                assertEquals(all[0], (byte) in.read());
            }

            byte[] pattern = new byte[] {all[3], all[4], all[5]};
            assertEquals(4L, blob.position(pattern, 1L));
            assertEquals(-1L, blob.position(new byte[] {(byte) 0xEE, (byte) 0xEF, (byte) 0xAB, (byte) 0xCD}, 1L));

            try (OleBlob other = new OleBlob.Builder()
                .withPackagePrettyName("Text File")
                .withPackageClassName("Text.File")
                .withPackageTypeName("TextFile")
                .withOtherBytes(part).toBlob()) {
                assertTrue(blob.position(other, 1L) != 0L);
            }

            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBinaryStream(1L));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.truncate(1L));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBytes(1L, DATA));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBytes(1L, DATA, 0, 2));

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            blob.writeTo(bout);
            assertEquals(len, bout.toByteArray().length);
            assertNotNull(blob.toString());

            blob.free();
            assertThrows(Exception.class, () -> blob.getBytes(1L, 1));
        }
    }

}
