/*
Copyright (c) 2014 James Ahlborn

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

/**
 * ColumnImpl subclass which is used for Memo data types.
 *
 * @author James Ahlborn
 */
class MemoColumnImpl extends LongValueColumnImpl {
    /** whether or not they are compressed */
    private final boolean   _compressedUnicode;
    /** the collating sort order for a text field */
    private final SortOrder _sortOrder;
    /** the code page for a text field (for certain db versions) */
    private final short     _codePage;
    /**
     * complex column which tracks the version history for this "append only" column
     */
    private ColumnImpl      _versionHistoryCol;
    /**
     * whether or not this is a hyperlink column (only possible for columns of type MEMO)
     */
    private final boolean   _hyperlink;

    MemoColumnImpl(InitArgs args) {
        super(args);

        // co-located w/ precision/scale
        _sortOrder = readSortOrder(
            args.buffer, args.offset + getFormat().OFFSET_COLUMN_SORT_ORDER,
            getFormat());
        _codePage = readCodePage(args.buffer, args.offset, getFormat());

        _compressedUnicode =
            (args.extFlags & COMPRESSED_UNICODE_EXT_FLAG_MASK) != 0;

        // only memo fields can be hyperlinks
        _hyperlink = (args.flags & HYPERLINK_FLAG_MASK) != 0;
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

    @Override
    public ColumnImpl getVersionHistoryColumn() {
        return _versionHistoryCol;
    }

    @Override
    public void setVersionHistoryColumn(ColumnImpl versionHistoryCol) {
        _versionHistoryCol = versionHistoryCol;
    }

    @Override
    public boolean isHyperlink() {
        return _hyperlink;
    }
}
