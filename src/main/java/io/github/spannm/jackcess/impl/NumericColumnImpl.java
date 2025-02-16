package io.github.spannm.jackcess.impl;

/**
 * ColumnImpl subclass which is used for numeric data types.
 */
class NumericColumnImpl extends ColumnImpl {
    /** Numeric precision */
    private final byte _precision;
    /** Numeric scale */
    private final byte _scale;

    NumericColumnImpl(InitArgs args) {
        super(args);

        _precision = args.buffer.get(args.offset + getFormat().OFFSET_COLUMN_PRECISION);
        _scale = args.buffer.get(args.offset + getFormat().OFFSET_COLUMN_SCALE);
    }

    @Override
    public byte getPrecision() {
        return _precision;
    }

    @Override
    public byte getScale() {
        return _scale;
    }
}
