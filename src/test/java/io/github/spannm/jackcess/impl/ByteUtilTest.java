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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
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
    void getUnsignedShort() {
        ByteBuffer bb = buffer(0xFF, 0xFF, 0x01, 0x00);
        assertThat(ByteUtil.getUnsignedShort(bb)).isEqualTo(0xFFFF);
        assertThat(bb.position()).isEqualTo(2);
        assertThat(ByteUtil.getUnsignedShort(bb)).isEqualTo(1);
        assertThat(bb.position()).isEqualTo(4);
        assertThat(ByteUtil.asUnsignedShort((short) -1)).isEqualTo(0xFFFF);
        assertThat(ByteUtil.asUnsignedByte((byte) -1)).isEqualTo(0xFF);
    }

    @Test
    void getUnsignedVarInt() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        assertThat(ByteUtil.getUnsignedVarInt(bb, 0, 1)).isEqualTo(1);
        assertThat(ByteUtil.getUnsignedVarInt(bb, 0, 2)).isEqualTo(0x0201);
        assertThat(ByteUtil.getUnsignedVarInt(bb, 0, 3)).isEqualTo(0x030201);
        assertThat(ByteUtil.getUnsignedVarInt(bb, 0, 4)).isEqualTo(0x04030201);
        assertThrows(IllegalArgumentException.class, () -> ByteUtil.getUnsignedVarInt(bb, 0, 5));

        assertThat(ByteUtil.getUnsignedVarInt(bb, 2)).isEqualTo(0x0201);
        assertThat(bb.position()).isEqualTo(2);
    }

    @Test
    void getBytesAndConcat() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        bb.position(1);
        assertThat(ByteUtil.getBytes(bb, 2)).containsExactly(new byte[]{0x02, 0x03});

        // offset variant restores the original position
        bb.position(1);
        assertThat(ByteUtil.getBytes(bb, 2, 2)).containsExactly(new byte[]{0x03, 0x04});
        assertThat(bb.position()).isEqualTo(1);

        assertThat(ByteUtil.concat(new byte[]{1, 2}, new byte[]{3, 4})).containsExactly(new byte[]{1, 2, 3, 4});
        assertThat(ByteUtil.copyOf(new byte[]{1, 2}, 3)).containsExactly(new byte[]{1, 2, 0});
        assertThat(ByteUtil.copyOf(new byte[]{1, 2}, 1, 1)).containsExactly(new byte[]{2});
    }

    @Test
    void clearRemaining() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        bb.position(2);
        ByteUtil.clearRemaining(bb);
        assertThat(bb.position()).isEqualTo(2);
        assertThat(bb.get(2)).isEqualTo((byte) 0);
        assertThat(bb.get(3)).isEqualTo((byte) 0);
    }

    @Test
    void findRange() {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06);
        assertThat(ByteUtil.findRange(bb, 0, new byte[]{0x03, 0x04})).isEqualTo(2);
        assertThat(ByteUtil.findRange(bb, 0, new byte[]{0x09, 0x09})).isEqualTo(-1);
        assertThat(ByteUtil.findRange(bb, 4, new byte[]{0x01, 0x02})).isEqualTo(-1);
    }

    @Test
    void hexString() throws Exception {
        ByteBuffer bb = buffer(0x01, 0x02, 0x03, 0x04);
        String hex = ByteUtil.toHexString(bb, 4);
        assertThat(hex.contains("01")).isTrue();
        assertThat(ByteUtil.toHexString(new byte[]{1, 2, 3})).isNotNull();

        ByteBuffer target = ByteBuffer.allocate(2);
        ByteUtil.writeHexString(target, "0A1B");
        assertThat(target.get(0)).isEqualTo((byte) 0x0A);
        assertThat(target.get(1)).isEqualTo((byte) 0x1B);
        assertThrows(IOException.class, () -> ByteUtil.writeHexString(ByteBuffer.allocate(2), "0A1"));

        File out = TestUtil.createTempFile(getShortTestMethodName(), ".hex", false);
        ByteUtil.toHexFile(out.getAbsolutePath(), bb, 0, 4);
        assertThat(Files.readString(out.toPath()).contains("01")).isTrue();
    }

    @Test
    void closeQuietly() {
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
    void byteStream() throws Exception {
        try (ByteUtil.ByteStream bs = new ByteUtil.ByteStream()) {
            bs.write("abc".getBytes(StandardCharsets.US_ASCII));
            assertThat(bs.getLength()).isEqualTo(3);
            assertThat(bs.toByteArray().length).isEqualTo(3);
            bs.reset();
            assertThat(bs.getLength()).isEqualTo(0);
        }
    }

}
