package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.JackcessRuntimeException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Default implementation of CodecProvider which does not have any actual encoding/decoding support. See
 * {@link CodecProvider} for details on a more useful implementation.
 */
public class DefaultCodecProvider implements CodecProvider {
    /** common instance of DefaultCodecProvider */
    public static final CodecProvider INSTANCE            = new DefaultCodecProvider();

    /** common instance of {@link DummyHandler} */
    public static final CodecHandler  DUMMY_HANDLER       = new DummyHandler();

    /** common instance of {@link UnsupportedHandler} */
    public static final CodecHandler  UNSUPPORTED_HANDLER = new UnsupportedHandler();

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns DUMMY_HANDLER for databases with no encoding and UNSUPPORTED_HANDLER for databases
     * with any encoding.
     */
    @Override
    public CodecHandler createHandler(PageChannel channel, Charset charset) throws IOException {
        JetFormat format = channel.getFormat();
        switch (format.CODEC_TYPE) {
            case NONE:
                // no encoding, all good
                return DUMMY_HANDLER;

            case JET:
            case OFFICE:
                // check for an encode key. if 0, not encoded
                ByteBuffer bb = channel.createPageBuffer();
                channel.readRootPage(bb);
                int codecKey = bb.getInt(format.OFFSET_ENCODING_KEY);
                return codecKey == 0 ? DUMMY_HANDLER : UNSUPPORTED_HANDLER;

            case MSISAM:
                // always encoded, we don't handle it
                return UNSUPPORTED_HANDLER;

            default:
                throw new JackcessRuntimeException("Unknown codec type " + format.CODEC_TYPE);
        }
    }

    /**
     * CodecHandler implementation which does nothing, useful for databases with no extra encoding.
     */
    public static class DummyHandler implements CodecHandler {
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
            // does nothing
        }

        @Override
        public ByteBuffer encodePage(ByteBuffer page, int pageNumber, int pageOffset) {
            // does nothing
            return page;
        }
    }

    /**
     * CodecHandler implementation which always throws UnsupportedCodecException, useful for databases with unsupported
     * encodings.
     */
    public static class UnsupportedHandler implements CodecHandler {
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
            throw new UnsupportedCodecException("Decoding not supported.  Please choose a CodecProvider which supports reading the current database encoding.");
        }

        @Override
        public ByteBuffer encodePage(ByteBuffer page, int pageNumber, int pageOffset) {
            throw new UnsupportedCodecException("Encoding not supported.  Please choose a CodecProvider which supports writing the current database encoding.");
        }
    }

}
