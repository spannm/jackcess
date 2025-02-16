package io.github.spannm.jackcess.impl;

/**
 * Exception thrown by a CodecHandler to indicate that the current encoding is not supported. This generally indicates
 * that a different CodecProvider needs to be chosen.
 */
public class UnsupportedCodecException extends UnsupportedOperationException {
    private static final long serialVersionUID = 20120313L;

    public UnsupportedCodecException(String msg) {
        super(msg);
    }

    public UnsupportedCodecException(String msg, Throwable t) {
        super(msg, t);
    }

    public UnsupportedCodecException(Throwable t) {
        super(t);
    }
}
