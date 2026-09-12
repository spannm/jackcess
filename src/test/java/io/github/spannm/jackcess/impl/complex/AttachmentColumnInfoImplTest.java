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

import io.github.spannm.jackcess.complex.Attachment;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

@SuppressWarnings("deprecation")
class AttachmentColumnInfoImplTest extends AbstractBaseTest {

    private static final byte[] DATA = getAsciiBytes("standalone attachment test data");

    @Test
    void testNewAttachmentPlain() throws IOException {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertArrayEquals(DATA, a.getFileData());
        assertNull(a.getFileName());
        assertNull(a.getFileUrl());
        assertNull(a.getFileType());
    }

    @Test
    void testNewAttachmentFull() throws IOException {
        Date now = new Date();
        Attachment a = AttachmentColumnInfoImpl.newAttachment(
            "http://example.com", "some.txt", "txt", DATA, now, 42);
        assertArrayEquals(DATA, a.getFileData());
        assertEquals("http://example.com", a.getFileUrl());
        assertEquals("some.txt", a.getFileName());
        assertEquals("txt", a.getFileType());
        assertEquals(now, a.getFileTimeStamp());
        assertEquals(42, a.getFileFlags());
    }

    @Test
    void testNewEncodedAttachmentPlain() throws IOException {
        byte[] encoded = AttachmentColumnInfoImpl.newAttachment("some.txt", "some.txt", "txt", DATA, null, null)
            .getEncodedFileData();
        Attachment a = AttachmentColumnInfoImpl.newEncodedAttachment(encoded);
        assertArrayEquals(encoded, a.getEncodedFileData());
    }

    @Test
    void testNewEncodedAttachmentFull() throws IOException {
        byte[] encoded = AttachmentColumnInfoImpl.newAttachment("some.txt", "some.txt", "txt", DATA, null, null)
            .getEncodedFileData();
        Date now = new Date();
        Attachment a = AttachmentColumnInfoImpl.newEncodedAttachment(
            "http://example.com", "some.txt", "txt", encoded, now, 7);
        assertEquals("some.txt", a.getFileName());
        assertEquals(now, a.getFileTimeStamp());
        assertEquals(7, a.getFileFlags());
        assertArrayEquals(DATA, a.getFileData());
    }

    @Test
    void testUrlAccessor() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertNull(a.getFileUrl());
        a.setFileUrl("http://example.com/file");
        assertEquals("http://example.com/file", a.getFileUrl());
    }

    @Test
    void testDateTimeStampAccessors() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertNull(a.getFileTimeStamp());
        Date now = new Date();
        a.setFileTimeStamp(now);
        assertEquals(now, a.getFileTimeStamp());
    }

    @Test
    void testLocalDateTimeStampAccessors() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertNull(a.getFileLocalTimeStamp());
        LocalDateTime now = LocalDateTime.now();
        a.setFileLocalTimeStamp(now);
        assertEquals(now, a.getFileLocalTimeStamp());
        assertEquals(now, a.getFileTimeStampObject());
    }

    @Test
    void testFlagsAccessor() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        assertNull(a.getFileFlags());
        a.setFileFlags(13);
        assertEquals(13, a.getFileFlags());
    }

    @Test
    void testSetEncodedFileDataClearsPlainData() throws IOException {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(DATA);
        byte[] encoded = a.getEncodedFileData();
        assertNotNull(encoded);

        Attachment other = AttachmentColumnInfoImpl.newAttachment(getAsciiBytes("other data"));
        byte[] otherEncoded = other.getEncodedFileData();

        a.setEncodedFileData(otherEncoded);
        assertArrayEquals(otherEncoded, a.getEncodedFileData());
        assertArrayEquals(getAsciiBytes("other data"), a.getFileData());
    }

    @Test
    void testToString() {
        Attachment a = AttachmentColumnInfoImpl.newAttachment(
            "http://example.com", "some.txt", "txt", DATA, new Date(), 1);
        String str = a.toString();
        assertTrue(str.contains("some.txt"));
        assertTrue(str.contains("http://example.com"));
        assertTrue(str.contains("txt"));
    }

    private static byte[] getAsciiBytes(String str) {
        return str.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
