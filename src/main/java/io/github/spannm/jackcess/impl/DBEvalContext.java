/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.EvalConfig;
import io.github.spannm.jackcess.expr.FunctionLookup;
import io.github.spannm.jackcess.expr.NumericConfig;
import io.github.spannm.jackcess.expr.TemporalConfig;
import io.github.spannm.jackcess.impl.expr.DefaultFunctions;
import io.github.spannm.jackcess.impl.expr.Expressionator;
import io.github.spannm.jackcess.impl.expr.NumberFormatter;
import io.github.spannm.jackcess.impl.expr.RandomContext;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;

public class DBEvalContext implements Expressionator.ParseContext, EvalConfig {
    private static final int               MAX_CACHE_SIZE = 10;

    private final DatabaseImpl             db;
    private FunctionLookup                 funcs          = DefaultFunctions.LOOKUP;
    private Map<String, DateTimeFormatter> sdfs;
    private Map<String, DecimalFormat>     dfs;
    private TemporalConfig                 temporal       = TemporalConfig.US_TEMPORAL_CONFIG;
    private NumericConfig                  numeric        = NumericConfig.US_NUMERIC_CONFIG;
    private final RandomContext            rndCtx         = new RandomContext();
    private Bindings                       bindings       = new SimpleBindings();

    public DBEvalContext(DatabaseImpl db) {
        this.db = db;
    }

    protected DatabaseImpl getDatabase() {
        return db;
    }

    @Override
    public TemporalConfig getTemporalConfig() {
        return temporal;
    }

    @Override
    public void setTemporalConfig(TemporalConfig newTemporal) {
        if (temporal != newTemporal) {
            temporal = newTemporal;
            sdfs = null;
        }
    }

    @Override
    public ZoneId getZoneId() {
        return db.getZoneId();
    }

    @Override
    public NumericConfig getNumericConfig() {
        return numeric;
    }

    @Override
    public void setNumericConfig(NumericConfig newNumeric) {
        if (numeric != newNumeric) {
            numeric = newNumeric;
            dfs = null;
        }
    }

    @Override
    public FunctionLookup getFunctionLookup() {
        return funcs;
    }

    @Override
    public void setFunctionLookup(FunctionLookup lookup) {
        funcs = lookup;
    }

    @Override
    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    @Override
    public DateTimeFormatter createDateFormatter(String formatStr) {
        if (sdfs == null) {
            sdfs = new SimpleCache<>(MAX_CACHE_SIZE);
        }
        DateTimeFormatter sdf = sdfs.get(formatStr);
        if (sdf == null) {
            sdf = DateTimeFormatter.ofPattern(formatStr, temporal.getLocale());
            sdfs.put(formatStr, sdf);
        }
        return sdf;
    }

    @Override
    public DecimalFormat createDecimalFormat(String formatStr) {
        if (dfs == null) {
            dfs = new SimpleCache<>(MAX_CACHE_SIZE);
        }
        DecimalFormat df = dfs.get(formatStr);
        if (df == null) {
            df = new DecimalFormat(formatStr, numeric.getDecimalFormatSymbols());
            df.setRoundingMode(NumberFormatter.ROUND_MODE);
            dfs.put(formatStr, df);
        }
        return df;
    }

    public float getRandom(Integer seed) {
        return rndCtx.getRandom(seed);
    }
}
