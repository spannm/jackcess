package io.github.spannm.jackcess.impl;

/**
 * ColumnImpl subclass which is used for Text data types.
 */
class TextColumnImpl extends ColumnImpl {
    /** whether or not they are compressed */
    private final boolean   _compressedUnicode;
    /** the collating sort order for a text field */
    private final SortOrder _sortOrder;
    /** the code page for a text field (for certain db versions) */
    private final short     _codePage;

    TextColumnImpl(InitArgs args) {
        super(args);

        // co-located w/ precision/scale
        _sortOrder = readSortOrder(args.buffer, args.offset + getFormat().OFFSET_COLUMN_SORT_ORDER, getFormat());
        _codePage = readCodePage(args.buffer, args.offset, getFormat());

        _compressedUnicode = (args.extFlags & COMPRESSED_UNICODE_EXT_FLAG_MASK) != 0;
    }

    @Override
    public boolean isCompressedUnicode() {
        return _compressedUnicode;
    }

    @Override
    public short getTextCodePage() {
        return _codePage;
    }

    @Override
    public SortOrder getTextSortOrder() {
        return _sortOrder;
    }
}
