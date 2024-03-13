/*
Copyright (c) 2012 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

/**
 * @author James Ahlborn
 */
class MemFileChannelTest extends AbstractBaseTest {

    @Test
    void testReadOnlyChannel() throws Exception {
        File testFile = new File(DIR_TEST_DATA, "V1997/compIndexTestV1997.mdb");
        try (MemFileChannel ch = MemFileChannel.newChannel(testFile, "r")) {
            assertEquals(testFile.length(), ch.size());
            assertEquals(0L, ch.position());

            assertThrows(NonWritableChannelException.class, () -> {
                ByteBuffer bb = ByteBuffer.allocate(1024);
                ch.write(bb);
            });

            assertThrows(NonWritableChannelException.class, () -> ch.truncate(0L));

            assertThrows(NonWritableChannelException.class, () -> ch.transferFrom(null, 0L, 10L));

            assertEquals(testFile.length(), ch.size());
            assertEquals(0L, ch.position());
        }
    }

    @Test
    void testChannel() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        try (MemFileChannel ch1 = MemFileChannel.newChannel()) {
            assertTrue(ch1.isOpen());
            assertEquals(0L, ch1.size());
            assertEquals(0L, ch1.position());
            assertEquals(-1, ch1.read(bb));
        }

        File testFile = new File(DIR_TEST_DATA, "V1997/compIndexTestV1997.mdb");
        MemFileChannel ch2 = MemFileChannel.newChannel(testFile, "r");
        assertEquals(testFile.length(), ch2.size());
        assertEquals(0L, ch2.position());

        assertThrows(IllegalArgumentException.class, () -> ch2.position(-1));

        MemFileChannel ch3 = MemFileChannel.newChannel();
        ch2.transferTo(ch3);
        ch3.force(true);
        assertEquals(testFile.length(), ch3.size());
        assertEquals(testFile.length(), ch3.position());

        assertThrows(IllegalArgumentException.class, () -> ch3.truncate(-1L));

        long trucSize = ch3.size() / 3;
        ch3.truncate(trucSize);
        assertEquals(trucSize, ch3.size());
        assertEquals(trucSize, ch3.position());
        ch3.position(0L);
        copy(ch2, ch3, bb);

        File tmpFile = File.createTempFile("chtest_", ".dat");
        tmpFile.deleteOnExit();

        try (FileOutputStream fc = new FileOutputStream(tmpFile)) {
            ch3.transferTo(fc);
        }

        assertEquals(testFile.length(), tmpFile.length());

        assertArrayEquals(TestUtil.toByteArray(testFile), TestUtil.toByteArray(tmpFile));

        ch3.truncate(0L);
        assertTrue(ch3.isOpen());
        assertEquals(0L, ch3.size());
        assertEquals(0L, ch3.position());
        assertEquals(-1, ch3.read(bb));

        ch3.close();
        assertFalse(ch3.isOpen());
    }

    private static void copy(FileChannel src, FileChannel dst, ByteBuffer bb) throws IOException {
        src.position(0L);
        while (true) {
            bb.clear();
            if (src.read(bb) < 0) {
                break;
            }
            bb.flip();
            dst.write(bb);
        }
    }

}
