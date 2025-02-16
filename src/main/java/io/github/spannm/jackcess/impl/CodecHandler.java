package io.github.spannm.jackcess.impl;

import java.nio.ByteBuffer;

/**
 * Interface for a handler which can encode/decode a specific access page encoding.
 */
public interface CodecHandler {
    /**
     * Returns {@code true} if this handler can encode partial pages, {@code false} otherwise. If this method returns
     * {@code false}, the {@link #encodePage} method will never be called with a non-zero pageOffset.
     */
    boolean canEncodePartialPage();

    /**
     * Returns {@code true} if this handler can decode a page inline, {@code false} otherwise. If this method returns
     * {@code false}, the {@link #decodePage} method will always be called with separate buffers.
     */
    boolean canDecodeInline();

    /**
     * Decodes the given page buffer.
     *
     * @param inPage the page to be decoded
     * @param outPage the decoded page. if {@link #canDecodeInline} is {@code
     *                true}, this will be the same buffer as inPage.
     * @param pageNumber the page number of the given page
     */
    void decodePage(ByteBuffer inPage, ByteBuffer outPage, int pageNumber);

    /**
     * Encodes the given page buffer into a new page buffer and returns it. The returned page buffer will be used
     * immediately and discarded so that it may be re-used for subsequent page encodings.
     *
     * @param page the page to be encoded, should not be modified
     * @param pageNumber the page number of the given page
     * @param pageOffset offset within the page at which to start writing the page data
     *
     * @return the properly encoded page buffer for the given page buffer
     */
    ByteBuffer encodePage(ByteBuffer page, int pageNumber, int pageOffset);
}
