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
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Tests for {@link ReadOnlyFileChannel} and additional {@link MemFileChannel} functionality.
 */
class FileChannelTest extends AbstractBaseTest {

    private static final byte[] DATA = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private File createDataFile() throws IOException {
        File f = TestUtil.createTempFile(getShortTestMethodName(), ".bin", false);
        Files.write(f.toPath(), DATA);
        return f;
    }

    @Test
    void readOnlyFileChannel() throws Exception {
        File f = createDataFile();
        try (FileChannel delegate = FileChannel.open(f.toPath(), StandardOpenOption.READ);
             ReadOnlyFileChannel ch = new ReadOnlyFileChannel(delegate)) {

            assertThat(ch.size()).isEqualTo(DATA.length);
            assertThat(ch.position()).isEqualTo(0L);

            ByteBuffer buf = ByteBuffer.allocate(4);
            assertThat(ch.read(buf)).isEqualTo(4);
            assertThat(ch.position()).isEqualTo(4L);

            ByteBuffer buf2 = ByteBuffer.allocate(2);
            ByteBuffer buf3 = ByteBuffer.allocate(2);
            assertThat(ch.read(new ByteBuffer[]{buf2, buf3}, 0, 2)).isEqualTo(4L);

            ByteBuffer posBuf = ByteBuffer.allocate(2);
            assertThat(ch.read(posBuf, 0L)).isEqualTo(2);

            assertThat(ch.position(0L)).isSameAs(ch);
            assertThat(ch.position()).isEqualTo(0L);

            ch.force(true);

            MemFileChannel target = MemFileChannel.newChannel();
            assertThat(ch.transferTo(0L, DATA.length, target)).isEqualTo(DATA.length);
            target.close();

            ByteBuffer src = ByteBuffer.wrap(DATA);
            assertThrows(NonWritableChannelException.class, () -> ch.truncate(1L));
            assertThrows(NonWritableChannelException.class, () -> ch.write(src));
            assertThrows(NonWritableChannelException.class, () -> ch.write(src, 0L));
            assertThrows(NonWritableChannelException.class, () -> ch.write(new ByteBuffer[] {src}, 0, 1));
            assertThrows(NonWritableChannelException.class, () -> ch.transferFrom(delegate, 0L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> ch.map(FileChannel.MapMode.READ_ONLY, 0L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> ch.lock(0L, 1L, true));
            assertThrows(UnsupportedOperationException.class, () -> ch.tryLock(0L, 1L, true));
        }
    }

    @Test
    void memFileChannelFactories() throws Exception {
        File f = createDataFile();

        try (MemFileChannel ch = MemFileChannel.newChannel(f)) {
            assertThat(ch.size()).isEqualTo(DATA.length);
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath())) {
            assertThat(ch.size()).isEqualTo(DATA.length);
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath(), StandardOpenOption.READ)) {
            assertThat(ch.size()).isEqualTo(DATA.length);
            assertThrows(NonWritableChannelException.class, () -> ch.write(ByteBuffer.wrap(DATA)));
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath(), StandardOpenOption.WRITE)) {
            assertThat(ch.write(ByteBuffer.wrap(DATA), 0L)).isEqualTo(DATA.length);
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(new ByteArrayInputStream(DATA))) {
            assertThat(ch.size()).isEqualTo(DATA.length);
        }
        try (FileChannel in = FileChannel.open(f.toPath(), StandardOpenOption.READ);
             MemFileChannel ch = MemFileChannel.newChannel(in)) {
            assertThat(ch.size()).isEqualTo(DATA.length);
        }
    }

    @Test
    void memFileChannelScatterGather() throws Exception {
        try (MemFileChannel ch = MemFileChannel.newChannel()) {
            ByteBuffer b1 = ByteBuffer.wrap(DATA, 0, 8);
            ByteBuffer b2 = ByteBuffer.wrap(DATA, 8, 8);
            assertThat(ch.write(new ByteBuffer[]{b1, b2}, 0, 2)).isEqualTo(DATA.length);

            ch.position(0L);
            ByteBuffer d1 = ByteBuffer.allocate(8);
            ByteBuffer d2 = ByteBuffer.allocate(8);
            assertThat(ch.read(new ByteBuffer[]{d1, d2}, 0, 2)).isEqualTo(DATA.length);

            // reading past the end
            assertThat(ch.read(new ByteBuffer[]{ByteBuffer.allocate(4)}, 0, 1)).isEqualTo(-1L);

            // transfer from a position beyond the end yields nothing
            try (MemFileChannel target = MemFileChannel.newChannel()) {
                assertThat(ch.transferTo(ch.size() + 10, 10L, target)).isEqualTo(0L);
            }

            assertThrows(UnsupportedOperationException.class, () -> ch.map(FileChannel.MapMode.READ_ONLY, 0L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> ch.lock(0L, 1L, true));
            assertThrows(UnsupportedOperationException.class, () -> ch.tryLock(0L, 1L, true));
        }
    }

}
