package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.*;

import java.util.Arrays;

public class FunctionSupport {
    private static final char NON_VAR_SUFFIX = '$';

    private FunctionSupport() {
    }

    public abstract static class BaseFunction implements Function {
        private final String _name;
        private final int    _minParams;
        private final int    _maxParams;

        protected BaseFunction(String name, int minParams, int maxParams) {
            _name = name;
            _minParams = minParams;
            _maxParams = maxParams;
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public boolean isPure() {
            // most functions are probably pure, so make this the default
            return true;
        }

        protected void validateNumParams(Value[] params) {
            int num = params.length;
            if (num < _minParams || num > _maxParams) {
                String range = _minParams == _maxParams ? "" + _minParams : _minParams + " to " + _maxParams;
                throw new EvalException("Invalid number of parameters " + num + " passed, expected " + range);
            }
        }

        protected EvalException invalidFunctionCall(Throwable t, Value[] params) {
            String paramStr = Arrays.toString(params);
            String msg = "Invalid function call {" + _name + "(" + paramStr.substring(1, paramStr.length() - 1) + ")}";
            return new EvalException(msg, t);
        }

        @Override
        public String toString() {
            return getName() + "()";
        }
    }

    public abstract static class Func0 extends BaseFunction {
        protected Func0(String name) {
            super(name, 0, 0);
        }

        @Override
        public boolean isPure() {
            // 0-arg functions are usually not pure
            return false;
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                return eval0(ctx);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value eval0(EvalContext ctx);
    }

    public abstract static class Func1 extends BaseFunction {
        protected Func1(String name) {
            super(name, 1, 1);
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                return eval1(ctx, params[0]);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value eval1(EvalContext ctx, Value param);
    }

    public abstract static class Func1NullIsNull extends BaseFunction {
        protected Func1NullIsNull(String name) {
            super(name, 1, 1);
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                Value param1 = params[0];
                if (param1.isNull()) {
                    return param1;
                }
                return eval1(ctx, param1);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value eval1(EvalContext ctx, Value param);
    }

    public abstract static class Func2 extends BaseFunction {
        protected Func2(String name) {
            super(name, 2, 2);
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                return eval2(ctx, params[0], params[1]);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value eval2(EvalContext ctx, Value param1, Value param2);
    }

    public abstract static class Func3 extends BaseFunction {
        protected Func3(String name) {
            super(name, 3, 3);
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                return eval3(ctx, params[0], params[1], params[2]);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value eval3(EvalContext ctx, Value param1, Value param2, Value param3);
    }

    public abstract static class FuncVar extends BaseFunction {
        protected FuncVar(String name) {
            super(name, 0, Integer.MAX_VALUE);
        }

        protected FuncVar(String name, int minParams, int maxParams) {
            super(name, minParams, maxParams);
        }

        @Override
        public final Value eval(EvalContext ctx, Value... params) {
            try {
                validateNumParams(params);
                return evalVar(ctx, params);
            } catch (Exception _ex) {
                throw invalidFunctionCall(_ex, params);
            }
        }

        protected abstract Value evalVar(EvalContext ctx, Value[] params);
    }

    public static class StringFuncWrapper implements Function {
        private final String   mname;
        private final Function mdelegate;

        public StringFuncWrapper(Function _delegate) {
            mdelegate = _delegate;
            mname = mdelegate.getName() + NON_VAR_SUFFIX;
        }

        @Override
        public String getName() {
            return mname;
        }

        @Override
        public boolean isPure() {
            return mdelegate.isPure();
        }

        @Override
        public Value eval(EvalContext ctx, Value... params) {
            Value result = mdelegate.eval(ctx, params);
            if (result.isNull()) {
                // non-variant version does not do null-propagation, so force
                // exception to be thrown here
                result.getAsString(ctx);
            }
            return result;
        }

        @Override
        public String toString() {
            return getName() + "()";
        }
    }

    public static boolean getOptionalBooleanParam(LocaleContext ctx, Value[] params, int idx) {
        return params.length > idx && params[idx].getAsBoolean(ctx);
    }

    public static double getOptionalDoubleParam(EvalContext ctx, Value[] params, int idx, double defValue) {
        return params.length > idx ? params[idx].getAsDouble(ctx) : defValue;
    }

    public static int getOptionalIntParam(LocaleContext ctx, Value[] params, int idx, int defValue) {
        return getOptionalIntParam(ctx, params, idx, defValue, defValue);
    }

    public static int getOptionalIntParam(LocaleContext ctx, Value[] params, int idx, int defValue, int useDefValue) {
        int val = defValue;
        if (params.length > idx) {
            val = params[idx].getAsLongInt(ctx);
            if (val == useDefValue) {
                val = defValue;
            }
        }
        return val;
    }

}
