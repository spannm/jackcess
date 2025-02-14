package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.impl.expr.Expressionator;

public class ColDefaultValueEvalContext extends ColEvalContext {
    public ColDefaultValueEvalContext(ColumnImpl col) {
        super(col);
    }

    ColDefaultValueEvalContext withExpr(String exprStr) {
        setExpr(Expressionator.Type.DEFAULT_VALUE, exprStr);
        return this;
    }

    @Override
    public Value.Type getResultType() {
        return toValueType(getCol().getType());
    }
}
