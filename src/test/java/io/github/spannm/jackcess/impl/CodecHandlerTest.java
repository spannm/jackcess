/*
Copyright (c) 2012 Health Market Science, Inc.

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

package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.impl.JetFormatTest.SUPPORTED_FILEFORMATS;

import io.github.spannm.jackcess.*;
import junit.framework.TestCase;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 *
 * @author James Ahlborn
 */
public class CodecHandlerTest extends TestCase {
    private static final CodecProvider SIMPLE_PROVIDER = (channel, charset) -> new SimpleCodecHandler(channel);
    private static final CodecProvider FULL_PROVIDER   = (channel, charset) -> new FullCodecHandler(channel);

    public CodecHandlerTest(String name) {
        super(name);
    }

    public void testCodecHandler() throws Exception {
        doTestCodecHandler(true);
        doTestCodecHandler(false);
    }

    private static void doTestCodecHandler(boolean simple) throws Exception {
        for (Database.FileFormat ff : SUPPORTED_FILEFORMATS) {
            Database db = TestUtil.createFile(ff);
            int pageSize = ((DatabaseImpl) db).getFormat().PAGE_SIZE;
            File dbFile = db.getFile();
            db.close();

            // apply encoding to file
            encodeFile(dbFile, pageSize, simple);

            db = new DatabaseBuilder(dbFile)
                .withCodecProvider(simple ? SIMPLE_PROVIDER : FULL_PROVIDER)
                .open();

            Table t1 = new TableBuilder("test1")
                .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
                .addColumn(new ColumnBuilder("data", DataType.TEXT).withLength(250))
                .withPrimaryKey("id")
                .addIndex(new IndexBuilder("data_idx").withColumns("data"))
                .toTable(db);

            Table t2 = new TableBuilder("test2")
                .addColumn(new ColumnBuilder("id", DataType.LONG).withAutoNumber(true))
                .addColumn(new ColumnBuilder("data", DataType.TEXT).withLength(250))
                .withPrimaryKey("id")
                .addIndex(new IndexBuilder("data_idx").withColumns("data"))
                .toTable(db);

            int autonum = 1;
            for (int i = 1; i < 2; ++i) {
                writeData(t1, t2, autonum, autonum + 100);
                autonum += 100;
            }

            db.close();
        }
    }

    private static void writeData(Table t1, Table t2, int start, int end)
        throws Exception {
        Database db = t1.getDatabase();
        ((DatabaseImpl) db).getPageChannel().startWrite();
        try {
            for (int i = start; i < end; ++i) {
                t1.addRow(null, "rowdata-" + i + TestUtil.createString(100));
                t2.addRow(null, "rowdata-" + i + TestUtil.createString(100));
            }
        } finally {
            ((DatabaseImpl) db).getPageChannel().finishWrite();
        }

        Cursor c1 = t1.newCursor().withIndex(t1.getPrimaryKeyIndex())
            .toCursor();
        Cursor c2 = t2.newCursor().withIndex(t2.getPrimaryKeyIndex())
            .toCursor();

        Iterator<? extends Row> i1 = c1.iterator();
        Iterator<? extends Row> i2 = c2.newIterable().reverse().iterator();

        int t1rows = 0;
        int t2rows = 0;
        ((DatabaseImpl) db).getPageChannel().startWrite();
        try {
            while (i1.hasNext() || i2.hasNext()) {
                if (i1.hasNext()) {
                    checkRow(i1.next());
                    i1.remove();
                    ++t1rows;
                }
                if (i2.hasNext()) {
                    checkRow(i2.next());
                    i2.remove();
                    ++t2rows;
                }
            }
        } finally {
            ((DatabaseImpl) db).getPageChannel().finishWrite();
        }

        assertEquals(100, t1rows);
        assertEquals(100, t2rows);
    }

    private static void checkRow(Row row) {
        int id = row.getInt("id");
        String value = row.getString("data");
        String valuePrefix = "rowdata-" + id;
        assertTrue(value.startsWith(valuePrefix));
        assertEquals(valuePrefix.length() + 100, value.length());
    }

    private static void encodeFile(File dbFile, int pageSize, boolean simple) throws Exception {
        long dbLen = dbFile.length();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(dbFile, "rw");
            FileChannel fileChannel = randomAccessFile.getChannel()) {
            ByteBuffer bb = ByteBuffer.allocate(pageSize)
                .order(PageChannel.DEFAULT_BYTE_ORDER);
            for (long offset = pageSize; offset < dbLen; offset += pageSize) {

                bb.clear();
                fileChannel.read(bb, offset);

                int pageNumber = (int) (offset / pageSize);
                if (simple) {
                    simpleEncode(bb.array(), bb.array(), pageNumber, 0, pageSize);
                } else {
                    fullEncode(bb.array(), bb.array(), pageNumber);
                }

                bb.rewind();
                fileChannel.write(bb, offset);
            }
        }
    }

    private static void simpleEncode(byte[] inBuffer, byte[] outBuffer,
        int pageNumber, int offset, int limit) {
        for (int i = offset; i < limit; ++i) {
            int mask = (i + pageNumber) % 256;
            outBuffer[i] = (byte) (inBuffer[i] ^ mask);
        }
    }

    private static void simpleDecode(byte[] inBuffer, byte[] outBuffer,
        int pageNumber) {
        simpleEncode(inBuffer, outBuffer, pageNumber, 0, inBuffer.length);
    }

    private static void fullEncode(byte[] inBuffer, byte[] outBuffer,
        int pageNumber) {
        int accum = 0;
        for (int i = 0; i < inBuffer.length; ++i) {
            int mask = (i + pageNumber + accum) % 256;
            accum += inBuffer[i];
            outBuffer[i] = (byte) (inBuffer[i] ^ mask);
        }
    }

    private static void fullDecode(byte[] inBuffer, byte[] outBuffer, int pageNumber) {
        int accum = 0;
        for (int i = 0; i < inBuffer.length; ++i) {
            int mask = (i + pageNumber + accum) % 256;
            outBuffer[i] = (byte) (inBuffer[i] ^ mask);
            accum += outBuffer[i];
        }
    }

    private static final class SimpleCodecHandler implements CodecHandler {
        private final TempBufferHolder _bufH = TempBufferHolder.newHolder(TempBufferHolder.Type.HARD, true);
        private final PageChannel      _channel;

        private SimpleCodecHandler(PageChannel channel) {
            _channel = channel;
        }

        @Override
        public boolean canEncodePartialPage() {
            return true;
        }

        @Override
        public boolean canDecodeInline() {
            return true;
        }

        @Override
        public void decodePage(ByteBuffer inPage, ByteBuffer outPage, int pageNumber) {
            byte[] arr = inPage.array();
            simpleDecode(arr, arr, pageNumber);
        }

        @Override
        public ByteBuffer encodePage(ByteBuffer page, int pageNumber, int pageOffset) {
            ByteBuffer bb = _bufH.getPageBuffer(_channel);
            bb.clear();
            simpleEncode(page.array(), bb.array(), pageNumber, pageOffset, page.limit());
            return bb;
        }
    }

    private static final class FullCodecHandler implements CodecHandler {
        private final TempBufferHolder _bufH = TempBufferHolder.newHolder(TempBufferHolder.Type.HARD, true);
        private final PageChannel      _channel;

        private FullCodecHandler(PageChannel channel) {
            _channel = channel;
        }

        @Override
        public boolean canEncodePartialPage() {
            return false;
        }

        @Override
        public boolean canDecodeInline() {
            return true;
        }

        @Override
        public void decodePage(ByteBuffer inPage, ByteBuffer outPage,
            int pageNumber) {
            byte[] arr = inPage.array();
            fullDecode(arr, arr, pageNumber);
        }

        @Override
        public ByteBuffer encodePage(ByteBuffer page, int pageNumber,
            int pageOffset) {
            assertEquals(0, pageOffset);
            assertEquals(_channel.getFormat().PAGE_SIZE, page.limit());

            ByteBuffer bb = _bufH.getPageBuffer(_channel);
            bb.clear();
            fullEncode(page.array(), bb.array(), pageNumber);
            return bb;
        }
    }

}
