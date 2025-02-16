package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.InvalidValueException;
import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.expr.Identifier;
import io.github.spannm.jackcess.expr.Value;
import io.github.spannm.jackcess.impl.expr.Expressionator;
import io.github.spannm.jackcess.util.ColumnValidator;

import java.io.IOException;

public class ColValidatorEvalContext extends ColEvalContext {
    private String _helpStr;
    private Object _val;

    public ColValidatorEvalContext(ColumnImpl col) {
        super(col);
    }

    ColValidatorEvalContext withExpr(String exprStr, String helpStr) {
        setExpr(Expressionator.Type.FIELD_VALIDATOR, exprStr);
        _helpStr = helpStr;
        return this;
    }

    ColumnValidator toColumnValidator(ColumnValidator delegate) {
        return new InternalColumnValidator(delegate) {
            @Override
            protected Object internalValidate(Column col, Object val) throws IOException {
                return ColValidatorEvalContext.this.validate(val);
            }

            @Override
            protected void appendToString(StringBuilder sb) {
                sb.append("expression=").append(ColValidatorEvalContext.this);
            }
        };
    }

    private void reset() {
        _val = null;
    }

    @Override
    public Value getThisColumnValue() {
        return toValue(_val);
    }

    @Override
    public Value getIdentifierValue(Identifier identifier) {
        // col validators can only get "this" column, but they can refer to it by
        // name
        if (!getCol().isThisColumn(identifier)) {
            throw new EvalException("Cannot access other fields for " + identifier);
        }
        return getThisColumnValue();
    }

    private Object validate(Object val) throws IOException {
        try {
            _val = val;
            Boolean result = (Boolean) eval();
            if (!result) {
                String msg = _helpStr != null ? _helpStr : "Invalid column value '" + val + "'";
                throw new InvalidValueException(withErrorContext(msg));
            }
            return val;
        } finally {
            reset();
        }
    }
}
