package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.util.ColumnValidator;
import io.github.spannm.jackcess.util.SimpleColumnValidator;

import java.io.IOException;

/**
 * Base class for ColumnValidator instances handling "internal" validation functionality, which are wrappers around any
 * "external" behavior.
 *
 * @author James Ahlborn
 */
abstract class InternalColumnValidator implements ColumnValidator {
    private ColumnValidator _delegate;

    protected InternalColumnValidator(ColumnValidator delegate) {
        _delegate = delegate;
    }

    ColumnValidator getExternal() {
        ColumnValidator extValidator = _delegate;
        while (extValidator instanceof InternalColumnValidator) {
            extValidator = ((InternalColumnValidator) extValidator)._delegate;
        }
        return extValidator;
    }

    void setExternal(ColumnValidator extValidator) {
        InternalColumnValidator intValidator = this;
        while (intValidator._delegate instanceof InternalColumnValidator) {
            intValidator = (InternalColumnValidator) intValidator._delegate;
        }
        intValidator._delegate = extValidator;
    }

    @Override
    public final Object validate(Column col, Object val) throws IOException {
        val = _delegate.validate(col, val);
        return internalValidate(col, val);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (_delegate instanceof InternalColumnValidator) {
            ((InternalColumnValidator) _delegate).appendToString(sb);
        } else if (_delegate != SimpleColumnValidator.INSTANCE) {
            sb.append("custom=").append(_delegate);
        }
        if (sb.length() > 1) {
            sb.append(';');
        }
        appendToString(sb);
        return sb.append('}')
            .toString();
    }

    protected abstract void appendToString(StringBuilder sb);

    protected abstract Object internalValidate(Column col, Object val) throws IOException;
}
