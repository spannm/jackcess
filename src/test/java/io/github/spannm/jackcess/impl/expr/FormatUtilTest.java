/*
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
package io.github.spannm.jackcess.impl.expr;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.expr.*;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 * Verifies that caching of custom (non-predefined) {@code Format()} patterns in {@link FormatUtil} does not return
 * stale results when a context's {@link NumericConfig}/{@link TemporalConfig} changes.
 */
class FormatUtilTest extends AbstractBaseTest {

    @Test
    void customFormatCacheReflectsConfigChanges() {
        MutableContext ctx = new MutableContext(NumericConfig.US_NUMERIC_CONFIG);

        // same custom format string, evaluated repeatedly under different numeric configs sharing the same
        // Bindings (and therefore the same underlying format cache)
        assertThat(fmt(ctx, "0.00", 1234.5)).isEqualTo("1234.50");

        ctx.numericConfig = new NumericConfig(2, true, false, true, 3, Locale.GERMANY);
        assertThat(fmt(ctx, "0.00", 1234.5)).isEqualTo("1234,50");

        ctx.numericConfig = NumericConfig.US_NUMERIC_CONFIG;
        assertThat(fmt(ctx, "0.00", 1234.5)).isEqualTo("1234.50");
    }

    private static String fmt(EvalContext ctx, String fmtStr, double value) {
        return FormatUtil.format(ctx, ValueSupport.toValue(value), fmtStr, 1, 0).getAsString(ctx);
    }

    private static final class MutableContext implements EvalContext {
        private final Bindings   bindings = new SimpleBindings();
        private final RandomContext rndCtx = new RandomContext();
        private NumericConfig    numericConfig;

        private MutableContext(NumericConfig numericConfig) {
            this.numericConfig = numericConfig;
        }

        @Override
        public TemporalConfig getTemporalConfig() {
            return TemporalConfig.US_TEMPORAL_CONFIG;
        }

        @Override
        public DateTimeFormatter createDateFormatter(String formatStr) {
            return DateTimeFormatter.ofPattern(formatStr, getTemporalConfig().getLocale());
        }

        @Override
        public ZoneId getZoneId() {
            return ZoneId.systemDefault();
        }

        @Override
        public NumericConfig getNumericConfig() {
            return numericConfig;
        }

        @Override
        public DecimalFormat createDecimalFormat(String formatStr) {
            return new DecimalFormat(formatStr, numericConfig.getDecimalFormatSymbols());
        }

        @Override
        public float getRandom(Integer seed) {
            return rndCtx.getRandom(seed);
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
            return bindings;
        }

        @Override
        public Object get(String key) {
            return bindings.get(key);
        }

        @Override
        public void put(String key, Object value) {
            bindings.put(key, value);
        }
    }
}
