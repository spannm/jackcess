package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.impl.expr.Expressionator;

import java.io.IOException;

public class CalcColEvalContext extends RowEvalContext {
    private final ColumnImpl _col;

    public CalcColEvalContext(ColumnImpl col) {
        super(col.getDatabase());
        _col = col;
    }

    CalcColEvalContext withExpr(String exprStr) {
        setExpr(Expressionator.Type.EXPRESSION, exprStr);
        return this;
    }

    @Override
    protected TableImpl getTable() {
        return _col.getTable();
    }

    @Override
    public Value.Type getResultType() {
        return toValueType(_col.getType());
    }

    public Object eval(Object[] row) throws IOException {
        try {
            setRow(row);
            return eval();
        } finally {
            reset();
        }
    }

    @Override
    protected String withErrorContext(String msg) {
        return _col.withErrorContext(msg);
    }
}
