package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.Value;

public abstract class ColEvalContext extends BaseEvalContext {
    private final ColumnImpl _col;

    public ColEvalContext(ColumnImpl col) {
        super(col.getDatabase().getEvalContext());
        _col = col;
    }

    protected ColumnImpl getCol() {
        return _col;
    }

    @Override
    protected String withErrorContext(String msg) {
        return _col.withErrorContext(msg);
    }

    protected Value toValue(Object val) {
        return toValue(val, _col.getType());
    }
}
