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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLFeatureNotSupportedException;

/**
 * Tests for {@link OleBlob.Builder} and the {@code OleBlob} implementation.
 */
class OleBlobBuilderTest extends AbstractBaseTest {

    private static final byte[] DATA = "some ole content".getBytes(StandardCharsets.US_ASCII);

    @Test
    void builderSimplePackageVariants() throws Exception {
        OleBlob.Builder b = new OleBlob.Builder().withSimplePackageBytes(DATA);
        assertThat(b.getType()).isEqualTo(OleBlob.ContentType.SIMPLE_PACKAGE);
        assertThat(b.getBytes()).containsExactly(DATA);
        assertThat(b.getContentLength()).isEqualTo(DATA.length);
        assertThat(b.getPrettyName()).isEqualTo(OleBlob.Builder.PACKAGE_PRETTY_NAME);
        assertThat(b.getClassName()).isEqualTo(OleBlob.Builder.PACKAGE_TYPE_NAME);

        b.withSimplePackageFileName("myfile.txt").withSimplePackageFilePath("/tmp/myfile.txt");
        assertThat(b.getFileName()).isEqualTo("myfile.txt");
        assertThat(b.getFilePath()).isEqualTo("/tmp/myfile.txt");

        try (OleBlob blob = b.toBlob()) {
            assertThat(blob.getContent().getType()).isEqualTo(OleBlob.ContentType.SIMPLE_PACKAGE);
            assertThat(blob.toString()).isNotNull();
        }
    }

    @Test
    void builderLinkVariants() throws Exception {
        OleBlob.Builder b = new OleBlob.Builder()
            .withLinkFileName("linked.txt")
            .withLinkPath("/tmp/linked.txt");
        assertThat(b.getType()).isEqualTo(OleBlob.ContentType.LINK);
        assertThat(b.getFileName()).isEqualTo("linked.txt");
        assertThat(b.getFilePath()).isEqualTo("/tmp/linked.txt");

        try (OleBlob blob = b.toBlob()) {
            OleBlob.LinkContent lc = (OleBlob.LinkContent) blob.getContent();
            assertThat(lc.getLinkPath()).isEqualTo("/tmp/linked.txt");
            assertThat(lc.toString()).isNotNull();
        }
    }

    @Test
    void builderOtherStream() throws Exception {
        OleBlob.Builder b = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOtherStream(new ByteArrayInputStream(DATA), DATA.length);
        assertThat(b.getType()).isEqualTo(OleBlob.ContentType.OTHER);
        assertThat(b.getStream()).isNotNull();

        try (OleBlob blob = b.toBlob()) {
            OleBlob.OtherContent oc = (OleBlob.OtherContent) blob.getContent();
            assertThat(oc.length()).isEqualTo(DATA.length);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            oc.writeTo(bout);
            assertThat(bout.toByteArray()).containsExactly(DATA);
            assertThat(oc.toString()).isNotNull();
        }
    }

    @Test
    void builderFromFiles() throws Exception {
        File sampleFile = new File(DIR_TEST_DATA, "sample-input.tab");
        try (OleBlob blob = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOther(sampleFile).toBlob()) {
            assertThat(blob.getContent().getType()).isEqualTo(OleBlob.ContentType.OTHER);
        }

        OleBlob.Builder lb = new OleBlob.Builder().withLink(sampleFile);
        assertThat(lb.getFileName()).isEqualTo(sampleFile.getName());
        try (OleBlob blob = lb.toBlob()) {
            OleBlob.LinkContent lc = (OleBlob.LinkContent) blob.getContent();
            try (InputStream in = lc.getLinkStream()) {
                assertThat(in.read() >= 0).isTrue();
            }
        }
    }

    @Test
    void unsupportedContentType() {
        OleBlob.Builder b = new OleBlob.Builder();
        assertThrows(IllegalArgumentException.class, b::toBlob);
    }

    @Test
    void blobSqlMethods() throws Exception {
        try (OleBlob blob = new OleBlob.Builder()
            .withPackagePrettyName("Text File")
            .withPackageClassName("Text.File")
            .withPackageTypeName("TextFile")
            .withOtherBytes(DATA).toBlob()) {
            long len = blob.length();
            assertThat(len > DATA.length).isTrue();

            byte[] all = blob.getBytes(1L, (int) len);
            assertThat(all.length).isEqualTo(len);

            byte[] part = blob.getBytes(2L, 4);
            assertThat(part.length).isEqualTo(4);

            try (InputStream in = blob.getBinaryStream()) {
                assertThat((byte) in.read()).isEqualTo(all[0]);
            }
            try (InputStream in = blob.getBinaryStream(1L, len)) {
                assertThat((byte) in.read()).isEqualTo(all[0]);
            }

            byte[] pattern = new byte[] {all[3], all[4], all[5]};
            assertThat(blob.position(pattern, 1L)).isEqualTo(4L);
            assertThat(blob.position(new byte[]{(byte) 0xEE, (byte) 0xEF, (byte) 0xAB, (byte) 0xCD}, 1L)).isEqualTo(-1L);

            try (OleBlob other = new OleBlob.Builder()
                .withPackagePrettyName("Text File")
                .withPackageClassName("Text.File")
                .withPackageTypeName("TextFile")
                .withOtherBytes(part).toBlob()) {
                assertThat(blob.position(other, 1L) != 0L).isTrue();
            }

            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBinaryStream(1L));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.truncate(1L));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBytes(1L, DATA));
            assertThrows(SQLFeatureNotSupportedException.class, () -> blob.setBytes(1L, DATA, 0, 2));

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            blob.writeTo(bout);
            assertThat(bout.toByteArray().length).isEqualTo(len);
            assertThat(blob.toString()).isNotNull();

            blob.free();
            assertThrows(Exception.class, () -> blob.getBytes(1L, 1));
        }
    }

}
