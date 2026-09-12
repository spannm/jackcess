/*
Copyright (c) 2007 Health Market Science, Inc.

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

package io.github.spannm.jackcess;

import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.impl.PageChannel;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

class TableTest extends AbstractBaseTest {

    private final PageChannel      pageChannel = new PageChannel(true) {
                                                };
    private final List<ColumnImpl> columns     = new ArrayList<>();
    private TestTable              testTable;
    private int                    varLenIdx;
    private int                    fixedOffset;

    private void reset() {
        testTable = null;
        columns.clear();
        varLenIdx = 0;
        fixedOffset = 0;
    }

    @Test
    void testCreateRow() throws Exception {
        reset();
        newTestColumn(DataType.INT, false);
        newTestColumn(DataType.TEXT, false);
        newTestColumn(DataType.TEXT, false);
        newTestTable();

        int colCount = columns.size();
        ByteBuffer buffer = createRow(9, "Tim", "McCune");

        assertThat(buffer.getShort()).isEqualTo((short) colCount);
        assertThat(buffer.getShort()).isEqualTo((short) 9);
        assertThat(buffer.get()).isEqualTo((byte) 'T');
        assertThat(buffer.getShort(22)).isEqualTo((short) 22);
        assertThat(buffer.getShort(24)).isEqualTo((short) 10);
        assertThat(buffer.getShort(26)).isEqualTo((short) 4);
        assertThat(buffer.getShort(28)).isEqualTo((short) 2);
        assertThat(buffer.get(30)).isEqualTo((byte) 7);
    }

    @Test
    void unicodeCompression() throws Exception {
        reset();
        newTestColumn(DataType.TEXT, false);
        newTestColumn(DataType.TEXT, false);
        newTestTable();

        String small = "this is a string";
        String smallNotAscii = "this is a string\0";
        String large = TestUtil.createString(30);
        String largeNotAscii = large + "\0";

        ByteBuffer[] buf1 = encodeColumns(small, large);
        ByteBuffer[] buf2 = encodeColumns(smallNotAscii, largeNotAscii);

        reset();
        newTestColumn(DataType.TEXT, true);
        newTestColumn(DataType.TEXT, true);
        newTestTable();

        ByteBuffer[] bufCmp1 = encodeColumns(small, large);
        ByteBuffer[] bufCmp2 = encodeColumns(smallNotAscii, largeNotAscii);

        assertThat(bufCmp1[0].remaining() + small.length() - 2).isEqualTo(buf1[0].remaining());
        assertThat(bufCmp1[1].remaining() + large.length() - 2).isEqualTo(buf1[1].remaining());

        for (int i = 0; i < buf2.length; i++) {
            assertThat(toBytes(bufCmp2[i])).containsExactly(toBytes(buf2[i]));
        }

        assertThat(List.of(decodeColumns(bufCmp1))).isEqualTo(List.of(small, large));
        assertThat(List.of(decodeColumns(bufCmp2))).isEqualTo(List.of(smallNotAscii, largeNotAscii));

    }

    private ByteBuffer createRow(Object... row) throws IOException {
        return testTable.createRow(row);
    }

    private ByteBuffer[] encodeColumns(Object... row) throws IOException {
        ByteBuffer[] result = new ByteBuffer[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            ColumnImpl col = columns.get(i);
            result[i] = col.write(row[i], testTable.getFormat().MAX_ROW_SIZE);
        }
        return result;
    }

    private Object[] decodeColumns(ByteBuffer[] buffers) throws IOException {
        Object[] result = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            ColumnImpl col = columns.get(i);
            result[i] = col.read(toBytes(buffers[i]));
        }
        return result;
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        buffer.rewind();
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

    private TableImpl newTestTable() {
        testTable = new TestTable();
        return testTable;
    }

    private void newTestColumn(DataType type, boolean compressedUnicode) {

        int nextColIdx = columns.size();
        int nextVarLenIdx = 0;
        int nextFixedOff = 0;

        if (type.isVariableLength()) {
            nextVarLenIdx = varLenIdx++;
        } else {
            nextFixedOff = fixedOffset;
            fixedOffset += type.getFixedSize();
        }

        ColumnImpl col = new ColumnImpl(null, null, type, nextColIdx, nextFixedOff,
            nextVarLenIdx) {
            @Override
            public TableImpl getTable() {
                return testTable;
            }

            @Override
            public JetFormat getFormat() {
                return getTable().getFormat();
            }

            @Override
            public PageChannel getPageChannel() {
                return getTable().getPageChannel();
            }

            @Override
            protected Charset getCharset() {
                return getFormat().CHARSET;
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getDefault();
            }

            @Override
            public boolean isCompressedUnicode() {
                return compressedUnicode;
            }
        };

        columns.add(col);
    }

    private class TestTable extends TableImpl {
        private TestTable() {
            super(true, columns);
        }

        public ByteBuffer createRow(Object... row) throws IOException {
            return super.createRow(row, getPageChannel().createPageBuffer());
        }

        @Override
        public PageChannel getPageChannel() {
            return pageChannel;
        }

        @Override
        public JetFormat getFormat() {
            return JetFormat.VERSION_4;
        }
    }
}
