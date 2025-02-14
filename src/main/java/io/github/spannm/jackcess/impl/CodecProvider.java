package io.github.spannm.jackcess.impl;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Interface for a provider which can generate CodecHandlers for various types of database encodings. The
 * {@link DefaultCodecProvider} is the default implementation of this inferface, but it does not have any actual
 * encoding/decoding support (due to possible export issues with calling encryption APIs). See the separate
 * <a href="https://sourceforge.net/projects/jackcessencrypt/">Jackcess Encrypt</a> project for an implementation of
 * this interface which supports various access database encryption types.
 *
 * @author James Ahlborn
 */
public interface CodecProvider {
    /**
     * Returns a new CodecHandler for the database associated with the given PageChannel.
     *
     * @param channel the PageChannel for a Database
     * @param charset the Charset for the Database
     *
     * @return a new CodecHandler, may not be {@code null}
     */
    CodecHandler createHandler(PageChannel channel, Charset charset) throws IOException;
}
