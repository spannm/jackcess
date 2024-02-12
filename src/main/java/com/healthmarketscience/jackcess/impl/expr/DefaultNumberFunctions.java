/*
Copyright (c) 2017 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess.impl.expr;

import java.math.BigDecimal;

import com.healthmarketscience.jackcess.expr.EvalContext;
import com.healthmarketscience.jackcess.expr.EvalException;
import com.healthmarketscience.jackcess.expr.Function;
import com.healthmarketscience.jackcess.expr.Value;
import static com.healthmarketscience.jackcess.impl.expr.DefaultFunctions.*;
import static com.healthmarketscience.jackcess.impl.expr.FunctionSupport.*;

/**
 *
 * @author James Ahlborn
 */
public class DefaultNumberFunctions
{

  private DefaultNumberFunctions() {}

  static void init() {
    // dummy method to ensure this class is loaded
  }

  public static final Function ABS = registerFunc(new Func1NullIsNull("Abs") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      Value.Type mathType = param1.getType();

      switch(mathType) {
      case DATE:
      case TIME:
      case DATE_TIME:
        // dates/times get converted to date doubles for arithmetic
        double result = Math.abs(param1.getAsDouble(ctx));
        return ValueSupport.toDateValueIfPossible(mathType, result);
      case LONG:
        return ValueSupport.toValue(Math.abs(param1.getAsLongInt(ctx)));
      case DOUBLE:
        return ValueSupport.toValue(Math.abs(param1.getAsDouble(ctx)));
      case STRING:
      case BIG_DEC:
        return ValueSupport.toValue(param1.getAsBigDecimal(ctx).abs(
                                            NumberFormatter.DEC_MATH_CONTEXT));
      default:
        throw new EvalException("Unexpected type " + mathType);
      }
    }
  });

  public static final Function ATAN = registerFunc(new Func1("Atan") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.atan(param1.getAsDouble(ctx)));
    }
  });

  public static final Function COS = registerFunc(new Func1("Cos") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.cos(param1.getAsDouble(ctx)));
    }
  });

  public static final Function EXP = registerFunc(new Func1("Exp") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.exp(param1.getAsDouble(ctx)));
    }
  });

  public static final Function FIX = registerFunc(new Func1NullIsNull("Fix") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      if(param1.getType().isIntegral()) {
        return param1;
      }
      return ValueSupport.toValue(param1.getAsDouble(ctx).intValue());
    }
  });

  public static final Function INT = registerFunc(new Func1NullIsNull("Int") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      if(param1.getType().isIntegral()) {
        return param1;
      }
      return ValueSupport.toValue((int)Math.floor(param1.getAsDouble(ctx)));
    }
  });

  public static final Function LOG = registerFunc(new Func1("Log") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.log(param1.getAsDouble(ctx)));
    }
  });

  public static final Function RND = registerFunc(new FuncVar("Rnd", 0, 1) {
    @Override
    public boolean isPure() {
      return false;
    }
    @Override
    protected Value evalVar(EvalContext ctx, Value[] params) {
      Integer seed = ((params.length > 0) ? params[0].getAsLongInt(ctx) : null);
      return ValueSupport.toValue(ctx.getRandom(seed));
    }
  });

  public static final Function ROUND = registerFunc(new FuncVar("Round", 1, 2) {
    @Override
    protected Value evalVar(EvalContext ctx, Value[] params) {
      Value param1 = params[0];
      if(param1.isNull()) {
        return ValueSupport.NULL_VAL;
      }
      if(param1.getType().isIntegral()) {
        return param1;
      }
      int scale = 0;
      if(params.length > 1) {
        scale = params[1].getAsLongInt(ctx);
      }
      BigDecimal bd = param1.getAsBigDecimal(ctx)
        .setScale(scale, NumberFormatter.ROUND_MODE);
      return ValueSupport.toValue(bd);
    }
  });

  public static final Function SGN = registerFunc(new Func1NullIsNull("Sgn") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      int val = 0;
      if(param1.getType().isIntegral()) {
        val = param1.getAsLongInt(ctx);
      } else {
        val = param1.getAsBigDecimal(ctx).signum();
      }
      return ((val > 0) ? ValueSupport.ONE_VAL :
              ((val < 0) ? ValueSupport.NEG_ONE_VAL :
               ValueSupport.ZERO_VAL));
    }
  });

  public static final Function SQR = registerFunc(new Func1("Sqr") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      double dv = param1.getAsDouble(ctx);
      if(dv < 0.0d) {
        throw new EvalException("Invalid value '" + dv + "'");
      }
      return ValueSupport.toValue(Math.sqrt(dv));
    }
  });

  public static final Function SIN = registerFunc(new Func1("Sin") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.sin(param1.getAsDouble(ctx)));
    }
  });

  public static final Function TAN = registerFunc(new Func1("Tan") {
    @Override
    protected Value eval1(EvalContext ctx, Value param1) {
      return ValueSupport.toValue(Math.tan(param1.getAsDouble(ctx)));
    }
  });
}
