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
package io.github.spannm.jackcess.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for constructing {@code byte[]s} where the final size of the data is not known beforehand. The API is
 * similar to {@code ByteBuffer} but the data is not actually written to a {@code byte[]} until {@link #toBuffer} or
 * {@link #toArray} is called.
 */
public class ByteArrayBuilder {
    private int              pos;
    private final List<Data> data = new ArrayList<>();

    public int position() {
        return pos;
    }

    public ByteArrayBuilder reserveInt() {
        return reserve(4);
    }

    public ByteArrayBuilder reserveShort() {
        return reserve(2);
    }

    public ByteArrayBuilder reserve(int bytes) {
        pos += bytes;
        return this;
    }

    public ByteArrayBuilder put(byte val) {
        return put(new ByteData(pos, val));
    }

    public ByteArrayBuilder putInt(int val) {
        return putInt(pos, val);
    }

    public ByteArrayBuilder putInt(int atPos, int val) {
        return put(new IntData(atPos, val));
    }

    public ByteArrayBuilder putShort(short val) {
        return putShort(pos, val);
    }

    public ByteArrayBuilder putShort(int atPos, short val) {
        return put(new ShortData(atPos, val));
    }

    public ByteArrayBuilder put(byte[] val) {
        return put(new BytesData(pos, val));
    }

    public ByteArrayBuilder put(ByteBuffer val) {
        return put(new BufData(pos, val));
    }

    private ByteArrayBuilder put(Data newData) {
        data.add(newData);
        int endPos = newData.getEndPos();
        if (endPos > pos) {
            pos = endPos;
        }
        return this;
    }

    public ByteBuffer toBuffer() {
        return toBuffer(PageChannel.wrap(new byte[pos]));
    }

    public ByteBuffer toBuffer(ByteBuffer buf) {
        for (Data d : data) {
            d.write(buf);
        }
        buf.rewind();
        return buf;
    }

    public byte[] toArray() {
        return toBuffer().array();
    }

    private abstract static class Data {
        private final int pos;

        protected Data(int pos) {
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

        public int getEndPos() {
            return getPos() + size();
        }

        public abstract int size();

        public abstract void write(ByteBuffer buf);
    }

    private static final class IntData extends Data {
        private final int val;

        private IntData(int pos, int val) {
            super(pos);
            this.val = val;
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.putInt(getPos(), val);
        }
    }

    private static final class ShortData extends Data {
        private final short val;

        private ShortData(int pos, short val) {
            super(pos);
            this.val = val;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.putShort(getPos(), val);
        }
    }

    private static final class ByteData extends Data {
        private final byte val;

        private ByteData(int pos, byte val) {
            super(pos);
            this.val = val;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.put(getPos(), val);
        }
    }

    private static final class BytesData extends Data {
        private final byte[] val;

        private BytesData(int pos, byte[] val) {
            super(pos);
            this.val = val;
        }

        @Override
        public int size() {
            return val.length;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.position(getPos());
            buf.put(val);
        }
    }

    private static final class BufData extends Data {
        private final ByteBuffer val;

        private BufData(int pos, ByteBuffer val) {
            super(pos);
            this.val = val;
        }

        @Override
        public int size() {
            return val.remaining();
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.position(getPos());
            buf.put(val);
        }
    }
}
