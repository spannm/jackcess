package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.JackcessException;
import io.github.spannm.jackcess.JackcessRuntimeException;
import io.github.spannm.jackcess.expr.*;
import io.github.spannm.jackcess.impl.expr.Expressionator;
import io.github.spannm.jackcess.impl.expr.ValueSupport;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import javax.script.Bindings;

public abstract class BaseEvalContext implements EvalContext {
    /** map of all non-string data types */
    private static final Map<DataType, Value.Type> TYPE_MAP = new EnumMap<>(DataType.class);

    static {
        TYPE_MAP.put(DataType.BOOLEAN, Value.Type.LONG);
        TYPE_MAP.put(DataType.BYTE, Value.Type.LONG);
        TYPE_MAP.put(DataType.INT, Value.Type.LONG);
        TYPE_MAP.put(DataType.LONG, Value.Type.LONG);
        TYPE_MAP.put(DataType.MONEY, Value.Type.DOUBLE);
        TYPE_MAP.put(DataType.FLOAT, Value.Type.DOUBLE);
        TYPE_MAP.put(DataType.DOUBLE, Value.Type.DOUBLE);
        TYPE_MAP.put(DataType.SHORT_DATE_TIME, Value.Type.DATE_TIME);
        TYPE_MAP.put(DataType.NUMERIC, Value.Type.BIG_DEC);
        TYPE_MAP.put(DataType.BIG_INT, Value.Type.BIG_DEC);
    }

    private final DBEvalContext _dbCtx;
    private Expression          _expr;

    protected BaseEvalContext(DBEvalContext dbCtx) {
        _dbCtx = dbCtx;
    }

    void setExpr(Expressionator.Type exprType, String exprStr) {
        _expr = new RawExpr(exprType, exprStr);
    }

    protected DatabaseImpl getDatabase() {
        return _dbCtx.getDatabase();
    }

    @Override
    public TemporalConfig getTemporalConfig() {
        return _dbCtx.getTemporalConfig();
    }

    @Override
    public DateTimeFormatter createDateFormatter(String formatStr) {
        return _dbCtx.createDateFormatter(formatStr);
    }

    @Override
    public ZoneId getZoneId() {
        return _dbCtx.getZoneId();
    }

    @Override
    public NumericConfig getNumericConfig() {
        return _dbCtx.getNumericConfig();
    }

    @Override
    public DecimalFormat createDecimalFormat(String formatStr) {
        return _dbCtx.createDecimalFormat(formatStr);
    }

    @Override
    public float getRandom(Integer seed) {
        return _dbCtx.getRandom(seed);
    }

    @Override
    public Value.Type getResultType() {
        return null;
    }

    @Override
    public Value getThisColumnValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getIdentifierValue(Identifier identifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bindings getBindings() {
        return _dbCtx.getBindings();
    }

    @Override
    public Object get(String key) {
        return _dbCtx.getBindings().get(key);
    }

    @Override
    public void put(String key, Object value) {
        _dbCtx.getBindings().put(key, value);
    }

    public Object eval() throws IOException {
        try {
            return _expr.eval(this);
        } catch (Exception _ex) {
            String msg = withErrorContext(_ex.getMessage());
            throw new JackcessException(msg, _ex);
        }
    }

    public void collectIdentifiers(Collection<Identifier> identifiers) {
        _expr.collectIdentifiers(identifiers);
    }

    @Override
    public String toString() {
        return _expr.toString();
    }

    protected Value toValue(Object val, DataType dType) {
        try {
            // expression engine always uses LocalDateTime, so force that date/time
            // type
            val = ColumnImpl.toInternalValue(dType, val, getDatabase(), ColumnImpl.LDT_DATE_TIME_FACTORY);
            if (val == null) {
                return ValueSupport.NULL_VAL;
            }

            Value.Type vType = toValueType(dType);
            switch (vType) {
                case STRING:
                    return ValueSupport.toValue(val.toString());
                case DATE:
                case TIME:
                case DATE_TIME:
                    return ValueSupport.toValue(vType, (LocalDateTime) val);
                case LONG:
                    Integer i = val instanceof Integer ? (Integer) val : ((Number) val).intValue();
                    return ValueSupport.toValue(i);
                case DOUBLE:
                    Double d = val instanceof Double ? (Double) val : ((Number) val).doubleValue();
                    return ValueSupport.toValue(d);
                case BIG_DEC:
                    BigDecimal bd = ColumnImpl.toBigDecimal(val, getDatabase());
                    return ValueSupport.toValue(bd);
                default:
                    throw new JackcessRuntimeException("Unexpected type " + vType);
            }
        } catch (IOException _ex) {
            throw new EvalException("Failed converting value to type " + dType, _ex);
        }
    }

    public static Value.Type toValueType(DataType dType) {
        Value.Type type = TYPE_MAP.get(dType);
        return type == null ? Value.Type.STRING : type;
    }

    protected abstract String withErrorContext(String msg);

    private class RawExpr implements Expression {
        private final Expressionator.Type _exprType;
        private final String              _exprStr;

        private RawExpr(Expressionator.Type exprType, String exprStr) {
            _exprType = exprType;
            _exprStr = exprStr;
        }

        private Expression getExpr() {
            // when the expression is parsed we replace the raw version
            Expression expr = Expressionator.parse(_exprType, _exprStr, getResultType(), _dbCtx);
            _expr = expr;
            return expr;
        }

        @Override
        public Object eval(EvalContext ctx) {
            return getExpr().eval(ctx);
        }

        @Override
        public String toDebugString(LocaleContext ctx) {
            return getExpr().toDebugString(ctx);
        }

        @Override
        public String toRawString() {
            return _exprStr;
        }

        @Override
        public String toCleanString(LocaleContext ctx) {
            return getExpr().toCleanString(ctx);
        }

        @Override
        public boolean isConstant() {
            return getExpr().isConstant();
        }

        @Override
        public void collectIdentifiers(Collection<Identifier> identifiers) {
            getExpr().collectIdentifiers(identifiers);
        }

        @Override
        public String toString() {
            return toRawString();
        }
    }
}
