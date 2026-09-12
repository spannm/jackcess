/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
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

import static io.github.spannm.jackcess.test.Basename.COMP_INDEX;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

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
    void readOnlyChannel(TestDb testDb) throws Exception {
        try (MemFileChannel ch = MemFileChannel.newChannel(testDb.getFile(), "r")) {
            assertThat(ch.size()).isEqualTo(testDb.getFile().length());
            assertThat(ch.position()).isEqualTo(0L);

            assertThrows(NonWritableChannelException.class, () -> {
                ByteBuffer bb = ByteBuffer.allocate(1024);
                ch.write(bb);
            });

            assertThrows(NonWritableChannelException.class, () -> ch.truncate(0L));

            assertThrows(NonWritableChannelException.class, () -> ch.transferFrom(null, 0L, 10L));

            assertThat(ch.size()).isEqualTo(testDb.getFile().length());
            assertThat(ch.position()).isEqualTo(0L);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMP_INDEX)
    void channel(TestDb testDb) throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        try (MemFileChannel ch = MemFileChannel.newChannel()) {
            assertThat(ch.isOpen()).isTrue();
            assertThat(ch.size()).isEqualTo(0L);
            assertThat(ch.position()).isEqualTo(0L);
            assertThat(ch.read(bb)).isEqualTo(-1);
        }

        try (MemFileChannel ch2 = MemFileChannel.newChannel(testDb.getFile(), "r");
            MemFileChannel ch3 = MemFileChannel.newChannel()) {

            assertThat(ch2.size()).isEqualTo(testDb.getFile().length());
            assertThat(ch2.position()).isEqualTo(0L);

            assertThrows(IllegalArgumentException.class, () -> ch2.position(-1));

            ch2.transferTo(ch3);
            ch3.force(true);
            assertThat(ch3.size()).isEqualTo(testDb.getFile().length());
            assertThat(ch3.position()).isEqualTo(testDb.getFile().length());

            assertThrows(IllegalArgumentException.class, () -> ch3.truncate(-1L));

            long trucSize = ch3.size() / 3;
            ch3.truncate(trucSize);
            assertThat(ch3.size()).isEqualTo(trucSize);
            assertThat(ch3.position()).isEqualTo(trucSize);
            ch3.position(0L);
            copy(ch2, ch3, bb);

            File tempFile = TestUtil.createTempFile("chtest", ".dat", false);

            try (FileOutputStream fc = new FileOutputStream(tempFile)) {
                ch3.transferTo(fc);
            }

            assertThat(tempFile.length()).isEqualTo(testDb.getFile().length());

            assertThat(Files.readAllBytes(tempFile.toPath())).containsExactly(Files.readAllBytes(testDb.getFile().toPath()));

            ch3.truncate(0L);
            assertThat(ch3.isOpen()).isTrue();
            assertThat(ch3.size()).isEqualTo(0L);
            assertThat(ch3.position()).isEqualTo(0L);
            assertThat(ch3.read(bb)).isEqualTo(-1);
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
