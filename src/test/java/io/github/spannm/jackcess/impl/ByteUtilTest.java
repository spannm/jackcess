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
package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Tests for {@link ByteUtil}.
 */
class ByteUtilTest extends AbstractBaseTest {

    private static ByteBuffer buffer(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }

    @Test
    void testGetUnsignedShort() {
        ByteBuffer bb = buffer(0xFF, 0xFF, 0x01, 0x00);
        assertEquals(0xFFFF, ByteUtil.getUnsignedShort(bb));
        assertEquals(2, bb.position());
        assertEquals(1, ByteUtil.getUnsignedShort(bb));
        assertEquals(4, bb.position());
        assertEquals(0xFFFF, ByteUtil.asUnsignedShort((short) -1));
        assertEquals(0xFF, ByteUtil.asUnsignedByte((byte) -1));
    }

    @Test
    void testGetUnsignedVarInt() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        assertEquals(1, ByteUtil.getUnsignedVarInt(bb, 0, 1));
        assertEquals(0x0201, ByteUtil.getUnsignedVarInt(bb, 0, 2));
        assertEquals(0x030201, ByteUtil.getUnsignedVarInt(bb, 0, 3));
        assertEquals(0x04030201, ByteUtil.getUnsignedVarInt(bb, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> ByteUtil.getUnsignedVarInt(bb, 0, 5));

        assertEquals(0x0201, ByteUtil.getUnsignedVarInt(bb, 2));
        assertEquals(2, bb.position());
    }

    @Test
    void testGetBytesAndConcat() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        bb.position(1);
        assertArrayEquals(new byte[] {0x02, 0x03}, ByteUtil.getBytes(bb, 2));

        // offset variant restores the original position
        bb.position(1);
        assertArrayEquals(new byte[] {0x03, 0x04}, ByteUtil.getBytes(bb, 2, 2));
        assertEquals(1, bb.position());

        assertArrayEquals(new byte[] {1, 2, 3, 4},
            ByteUtil.concat(new byte[] {1, 2}, new byte[] {3, 4}));
        assertArrayEquals(new byte[] {1, 2, 0}, ByteUtil.copyOf(new byte[] {1, 2}, 3));
        assertArrayEquals(new byte[] {2}, ByteUtil.copyOf(new byte[] {1, 2}, 1, 1));
    }

    @Test
    void testClearRemaining() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        bb.position(2);
        ByteUtil.clearRemaining(bb);
        assertEquals(2, bb.position());
        assertEquals(0, bb.get(2));
        assertEquals(0, bb.get(3));
    }

    @Test
    void testFindRange() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06);
        assertEquals(2, ByteUtil.findRange(bb, 0, new byte[] {0x03, 0x04}));
        assertEquals(-1, ByteUtil.findRange(bb, 0, new byte[] {0x09, 0x09}));
        assertEquals(-1, ByteUtil.findRange(bb, 4, new byte[] {0x01, 0x02}));
    }

    @Test
    void testHexString() throws IOException {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        String hex = ByteUtil.toHexString(bb, 4);
        assertTrue(hex.contains("01"));
        assertNotNull(ByteUtil.toHexString(new byte[] {1, 2, 3}));

        ByteBuffer target = ByteBuffer.allocate(2);
        ByteUtil.writeHexString(target, "0A1B");
        assertEquals(0x0A, target.get(0));
        assertEquals(0x1B, target.get(1));
        assertThrows(IOException.class, () -> ByteUtil.writeHexString(ByteBuffer.allocate(2), "0A1"));

        File out = TestUtil.createTempFile(getShortTestMethodName(), ".hex", false);
        ByteUtil.toHexFile(out.getAbsolutePath(), bb, 0, 4);
        assertTrue(Files.readString(out.toPath()).contains("01"));
    }

    @Test
    void testCloseQuietly() {
        ByteUtil.closeQuietly(null);
        ByteUtil.closeQuietly(new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        });
    }

    @Test
    void testByteStream() throws IOException {
        try (ByteUtil.ByteStream bs = new ByteUtil.ByteStream()) {
            bs.write("abc".getBytes(StandardCharsets.US_ASCII));
            assertEquals(3, bs.getLength());
            assertEquals(3, bs.toByteArray().length);
            bs.reset();
            assertEquals(0, bs.getLength());
        }
    }

}
