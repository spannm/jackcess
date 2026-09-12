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
package io.github.spannm.jackcess.impl.complex;

import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.complex.Attachment;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;

@SuppressWarnings("deprecation")
class AttachmentColumnInfoImplTest extends AbstractBaseTest {

    private static final byte[] DATA = getAsciiBytes("standalone attachment test data");

    @Test
    void newAttachmentPlain() throws Exception {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertThat(a.getFileData()).containsExactly(DATA);
        assertThat(a.getFileName()).isNull();
        assertThat(a.getFileUrl()).isNull();
        assertThat(a.getFileType()).isNull();
    }

    @Test
    void newAttachmentFull() throws Exception {
        Date now = new Date();
        Attachment a = AttachmentColumnInfoImpl.newAttachment(
            "http://example.com", "some.txt", "txt", DATA, now, 42);
        assertThat(a.getFileData()).containsExactly(DATA);
        assertThat(a.getFileUrl()).isEqualTo("http://example.com");
        assertThat(a.getFileName()).isEqualTo("some.txt");
        assertThat(a.getFileType()).isEqualTo("txt");
        assertThat(a.getFileTimeStamp()).isEqualTo(now);
        assertThat(a.getFileFlags()).isEqualTo(42);
    }

    @Test
    void newEncodedAttachmentPlain() throws Exception {
        byte[] encoded = AttachmentColumnInfoImpl.newAttachment("some.txt", "some.txt", "txt", DATA, null, null)
            .getEncodedFileData();
        Attachment a = AttachmentColumnInfoImpl.newEncodedAttachment(encoded);
        assertThat(a.getEncodedFileData()).containsExactly(encoded);
    }

    @Test
    void newEncodedAttachmentFull() throws Exception {
        byte[] encoded = AttachmentColumnInfoImpl.newAttachment("some.txt", "some.txt", "txt", DATA, null, null)
            .getEncodedFileData();
        Date now = new Date();
        Attachment a = AttachmentColumnInfoImpl.newEncodedAttachment(
            "http://example.com", "some.txt", "txt", encoded, now, 7);
        assertThat(a.getFileName()).isEqualTo("some.txt");
        assertThat(a.getFileTimeStamp()).isEqualTo(now);
        assertThat(a.getFileFlags()).isEqualTo(7);
        assertThat(a.getFileData()).containsExactly(DATA);
    }

    @Test
    void urlAccessor() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertThat(a.getFileUrl()).isNull();
        a.setFileUrl("http://example.com/file");
        assertThat(a.getFileUrl()).isEqualTo("http://example.com/file");
    }

    @Test
    void dateTimeStampAccessors() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertThat(a.getFileTimeStamp()).isNull();
        Date now = new Date();
        a.setFileTimeStamp(now);
        assertThat(a.getFileTimeStamp()).isEqualTo(now);
    }

    @Test
    void localDateTimeStampAccessors() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertThat(a.getFileLocalTimeStamp()).isNull();
        LocalDateTime now = LocalDateTime.now();
        a.setFileLocalTimeStamp(now);
        assertThat(a.getFileLocalTimeStamp()).isEqualTo(now);
        assertThat(a.getFileTimeStampObject()).isEqualTo(now);
    }

    @Test
    void flagsAccessor() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertThat(a.getFileFlags()).isNull();
        a.setFileFlags(13);
        assertThat(a.getFileFlags()).isEqualTo(13);
    }

    @Test
    void setEncodedFileDataClearsPlainData() throws Exception {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        byte[] encoded = a.getEncodedFileData();
        assertThat(encoded).isNotNull();

        Attachment other = AttachmentColumnInfoImpl.newAttachment(getAsciiBytes("other data"));
        byte[] otherEncoded = other.getEncodedFileData();

        a.setEncodedFileData(otherEncoded);
        assertThat(a.getEncodedFileData()).containsExactly(otherEncoded);
        assertThat(a.getFileData()).containsExactly(getAsciiBytes("other data"));
    }

    @Test
    void testToString() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(
            "http://example.com", "some.txt", "txt", DATA, new Date(), 1);
        String str = a.toString();
        assertThat(str.contains("some.txt")).isTrue();
        assertThat(str.contains("http://example.com")).isTrue();
        assertThat(str.contains("txt")).isTrue();
    }

    private static byte[] getAsciiBytes(String str) {
        return str.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
