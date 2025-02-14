package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.EvalException;
import io.github.spannm.jackcess.expr.Identifier;
import io.github.spannm.jackcess.expr.Value;

public abstract class RowEvalContext extends BaseEvalContext {
    private Object[] _row;

    public RowEvalContext(DatabaseImpl db) {
        super(db.getEvalContext());
    }

    protected void setRow(Object[] row) {
        _row = row;
    }

    protected void reset() {
        _row = null;
    }

    @Override
    public Value getIdentifierValue(Identifier identifier) {

        TableImpl table = getTable();

        // we only support getting column values in this table from the current
        // row
        if (!table.isThisTable(identifier) || identifier.getPropertyName() != null) {
            throw new EvalException("Cannot access fields outside this table for " + identifier);
        }

        ColumnImpl col = table.getColumn(identifier.getObjectName());

        Object val = col.getRowValue(_row);

        return toValue(val, col.getType());
    }

    protected abstract TableImpl getTable();
}
