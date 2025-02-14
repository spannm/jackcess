package io.github.spannm.jackcess.impl;

import java.nio.ByteOrder;

/**
 * ColumnImpl subclass which is used for unknown/unsupported data types.
 *
 * @author James Ahlborn
 */
class UnsupportedColumnImpl extends ColumnImpl {
    private final byte _originalType;

    UnsupportedColumnImpl(InitArgs args) {
        super(args);
        _originalType = args.colType;
    }

    @Override
    byte getOriginalDataType() {
        return _originalType;
    }

    @Override
    public Object read(byte[] data, ByteOrder order) {
        return rawDataWrapper(data);
    }
}
