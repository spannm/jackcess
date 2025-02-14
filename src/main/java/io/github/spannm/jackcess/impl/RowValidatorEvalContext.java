package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.InvalidValueException;
import io.github.spannm.jackcess.impl.expr.Expressionator;

import java.io.IOException;

public class RowValidatorEvalContext extends RowEvalContext {
    private final TableImpl _table;
    private String          _helpStr;

    public RowValidatorEvalContext(TableImpl table) {
        super(table.getDatabase());
        _table = table;
    }

    RowValidatorEvalContext withExpr(String exprStr, String helpStr) {
        setExpr(Expressionator.Type.RECORD_VALIDATOR, exprStr);
        _helpStr = helpStr;
        return this;
    }

    @Override
    protected TableImpl getTable() {
        return _table;
    }

    public void validate(Object[] row) throws IOException {
        try {
            setRow(row);
            Boolean result = (Boolean) eval();
            if (!result) {
                String msg = _helpStr != null ? _helpStr : "Invalid row";
                throw new InvalidValueException(withErrorContext(msg));
            }
        } finally {
            reset();
        }
    }

    @Override
    protected String withErrorContext(String msg) {
        return _table.withErrorContext(msg);
    }
}
