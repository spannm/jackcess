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

/**
 * ColumnImpl subclass which is used for Memo data types.
 */
class MemoColumnImpl extends LongValueColumnImpl {
    /** whether or not they are compressed */
    private final boolean   mcompressedUnicode;
    /** the collating sort order for a text field */
    private final SortOrder msortOrder;
    /** the code page for a text field (for certain db versions) */
    private final short     mcodePage;
    /**
     * complex column which tracks the version history for this "append only" column
     */
    private ColumnImpl      mversionHistoryCol;
    /**
     * whether or not this is a hyperlink column (only possible for columns of type MEMO)
     */
    private final boolean   mhyperlink;

    MemoColumnImpl(InitArgs args) {
        super(args);

        // co-located w/ precision/scale
        msortOrder = readSortOrder(args.buffer, args.offset + getFormat().OFFSET_COLUMN_SORT_ORDER, getFormat());
        mcodePage = readCodePage(args.buffer, args.offset, getFormat());

        mcompressedUnicode = (args.extFlags & COMPRESSED_UNICODE_EXT_FLAG_MASK) != 0;

        // only memo fields can be hyperlinks
        mhyperlink = (args.flags & HYPERLINK_FLAG_MASK) != 0;
    }

    @Override
    public boolean isCompressedUnicode() {
        return mcompressedUnicode;
    }

    @Override
    public short getTextCodePage() {
        return mcodePage;
    }

    @Override
    public SortOrder getTextSortOrder() {
        return msortOrder;
    }

    @Override
    public ColumnImpl getVersionHistoryColumn() {
        return mversionHistoryCol;
    }

    @Override
    public void setVersionHistoryColumn(ColumnImpl versionHistoryCol) {
        mversionHistoryCol = versionHistoryCol;
    }

    @Override
    public boolean isHyperlink() {
        return mhyperlink;
    }
}
