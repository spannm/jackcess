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
    void testReadOnlyFileChannel() throws IOException {
        File f = createDataFile();
        try (FileChannel delegate = FileChannel.open(f.toPath(), StandardOpenOption.READ);
             ReadOnlyFileChannel ch = new ReadOnlyFileChannel(delegate)) {

            assertEquals(DATA.length, ch.size());
            assertEquals(0L, ch.position());

            ByteBuffer buf = ByteBuffer.allocate(4);
            assertEquals(4, ch.read(buf));
            assertEquals(4L, ch.position());

            ByteBuffer buf2 = ByteBuffer.allocate(2);
            ByteBuffer buf3 = ByteBuffer.allocate(2);
            assertEquals(4L, ch.read(new ByteBuffer[] {buf2, buf3}, 0, 2));

            ByteBuffer posBuf = ByteBuffer.allocate(2);
            assertEquals(2, ch.read(posBuf, 0L));

            assertSame(ch, ch.position(0L));
            assertEquals(0L, ch.position());

            ch.force(true);

            MemFileChannel target = MemFileChannel.newChannel();
            assertEquals(DATA.length, ch.transferTo(0L, DATA.length, target));
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
    void testMemFileChannelFactories() throws IOException {
        File f = createDataFile();

        try (MemFileChannel ch = MemFileChannel.newChannel(f)) {
            assertEquals(DATA.length, ch.size());
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath())) {
            assertEquals(DATA.length, ch.size());
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath(), StandardOpenOption.READ)) {
            assertEquals(DATA.length, ch.size());
            assertThrows(NonWritableChannelException.class, () -> ch.write(ByteBuffer.wrap(DATA)));
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(f.toPath(), StandardOpenOption.WRITE)) {
            assertEquals(DATA.length, ch.write(ByteBuffer.wrap(DATA), 0L));
        }
        try (MemFileChannel ch = MemFileChannel.newChannel(new ByteArrayInputStream(DATA))) {
            assertEquals(DATA.length, ch.size());
        }
        try (FileChannel in = FileChannel.open(f.toPath(), StandardOpenOption.READ);
             MemFileChannel ch = MemFileChannel.newChannel(in)) {
            assertEquals(DATA.length, ch.size());
        }
    }

    @Test
    void testMemFileChannelScatterGather() throws IOException {
        try (MemFileChannel ch = MemFileChannel.newChannel()) {
            ByteBuffer b1 = ByteBuffer.wrap(DATA, 0, 8);
            ByteBuffer b2 = ByteBuffer.wrap(DATA, 8, 8);
            assertEquals(DATA.length, ch.write(new ByteBuffer[] {b1, b2}, 0, 2));

            ch.position(0L);
            ByteBuffer d1 = ByteBuffer.allocate(8);
            ByteBuffer d2 = ByteBuffer.allocate(8);
            assertEquals(DATA.length, ch.read(new ByteBuffer[] {d1, d2}, 0, 2));

            // reading past the end
            assertEquals(-1L, ch.read(new ByteBuffer[] {ByteBuffer.allocate(4)}, 0, 1));

            // transfer from a position beyond the end yields nothing
            try (MemFileChannel target = MemFileChannel.newChannel()) {
                assertEquals(0L, ch.transferTo(ch.size() + 10, 10L, target));
            }

            assertThrows(UnsupportedOperationException.class, () -> ch.map(FileChannel.MapMode.READ_ONLY, 0L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> ch.lock(0L, 1L, true));
            assertThrows(UnsupportedOperationException.class, () -> ch.tryLock(0L, 1L, true));
        }
    }

}
