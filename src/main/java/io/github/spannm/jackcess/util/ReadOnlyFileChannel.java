package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.Database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;

/**
 * Wrapper for existing FileChannel which is read-only.
 * <p>
 * Implementation note: this class is optimized for use with {@link Database}. Therefore not all methods may be
 * implemented.
 *
 * @author James Ahlborn
 */
public class ReadOnlyFileChannel extends FileChannel {
    private final FileChannel _delegate;

    public ReadOnlyFileChannel(FileChannel delegate) {
        _delegate = delegate;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return _delegate.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return _delegate.read(dsts, offset, length);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return _delegate.read(dst, position);
    }

    @Override
    public long position() throws IOException {
        return _delegate.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        _delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return _delegate.size();
    }

    @Override
    public FileChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    public void force(boolean metaData) {
        // do nothing
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return _delegate.transferTo(position, count, target);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) {
        throw new NonWritableChannelException();
    }

    @Override
    public int write(ByteBuffer src, long position) {
        throw new NonWritableChannelException();
    }

    @Override
    public int write(ByteBuffer src) {
        throw new NonWritableChannelException();
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) {
        throw new NonWritableChannelException();
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void implCloseChannel() throws IOException {
        _delegate.close();
    }
}
