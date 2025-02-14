package io.github.spannm.jackcess.util;

import static io.github.spannm.jackcess.test.Basename.COMP_INDEX;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Files;

class MemFileChannelTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMP_INDEX)
    void testReadOnlyChannel(TestDb testDb) throws IOException {
        try (MemFileChannel ch = MemFileChannel.newChannel(testDb.getFile(), "r")) {
            assertEquals(testDb.getFile().length(), ch.size());
            assertEquals(0L, ch.position());

            assertThrows(NonWritableChannelException.class, () -> {
                ByteBuffer bb = ByteBuffer.allocate(1024);
                ch.write(bb);
            });

            assertThrows(NonWritableChannelException.class, () -> ch.truncate(0L));

            assertThrows(NonWritableChannelException.class, () -> ch.transferFrom(null, 0L, 10L));

            assertEquals(testDb.getFile().length(), ch.size());
            assertEquals(0L, ch.position());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMP_INDEX)
    void testChannel(TestDb testDb) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        try (MemFileChannel ch = MemFileChannel.newChannel()) {
            assertTrue(ch.isOpen());
            assertEquals(0L, ch.size());
            assertEquals(0L, ch.position());
            assertEquals(-1, ch.read(bb));
        }

        try (MemFileChannel ch2 = MemFileChannel.newChannel(testDb.getFile(), "r");
            MemFileChannel ch3 = MemFileChannel.newChannel()) {

            assertEquals(testDb.getFile().length(), ch2.size());
            assertEquals(0L, ch2.position());

            assertThrows(IllegalArgumentException.class, () -> ch2.position(-1));

            ch2.transferTo(ch3);
            ch3.force(true);
            assertEquals(testDb.getFile().length(), ch3.size());
            assertEquals(testDb.getFile().length(), ch3.position());

            assertThrows(IllegalArgumentException.class, () -> ch3.truncate(-1L));

            long trucSize = ch3.size() / 3;
            ch3.truncate(trucSize);
            assertEquals(trucSize, ch3.size());
            assertEquals(trucSize, ch3.position());
            ch3.position(0L);
            copy(ch2, ch3, bb);

            File tempFile = TestUtil.createTempFile("chtest", ".dat", false);

            try (FileOutputStream fc = new FileOutputStream(tempFile)) {
                ch3.transferTo(fc);
            }

            assertEquals(testDb.getFile().length(), tempFile.length());

            assertArrayEquals(Files.readAllBytes(testDb.getFile().toPath()), Files.readAllBytes(tempFile.toPath()));

            ch3.truncate(0L);
            assertTrue(ch3.isOpen());
            assertEquals(0L, ch3.size());
            assertEquals(0L, ch3.position());
            assertEquals(-1, ch3.read(bb));
        }
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
