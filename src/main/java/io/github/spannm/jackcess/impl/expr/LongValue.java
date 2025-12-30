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
package io.github.spannm.jackcess.impl.expr;

import io.github.spannm.jackcess.expr.LocaleContext;

import java.math.BigDecimal;

public class LongValue extends BaseNumericValue {
    private final Integer val;

    public LongValue(Integer _val) {
        val = _val;
    }

    @Override
    public Type getType() {
        return Type.LONG;
    }

    @Override
    public Object get() {
        return val;
    }

    @Override
    protected Number getNumber() {
        return val;
    }

    @Override
    public boolean getAsBoolean(LocaleContext ctx) {
        return val.longValue() != 0L;
    }

    @Override
    public Integer getAsLongInt(LocaleContext ctx) {
        return val;
    }

    @Override
    public BigDecimal getAsBigDecimal(LocaleContext ctx) {
        return BigDecimal.valueOf(val);
    }

    @Override
    public String getAsString(LocaleContext ctx) {
        return val.toString();
    }
}
