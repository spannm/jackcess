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

/**
 * ColumnImpl subclass which is used for numeric data types.
 */
class NumericColumnImpl extends ColumnImpl {
    /** Numeric precision */
    private final byte precision;
    /** Numeric scale */
    private final byte scale;

    NumericColumnImpl(InitArgs args) {
        super(args);

        precision = args.buffer.get(args.offset + getFormat().OFFSET_COLUMN_PRECISION);
        scale = args.buffer.get(args.offset + getFormat().OFFSET_COLUMN_SCALE);
    }

    @Override
    public byte getPrecision() {
        return precision;
    }

    @Override
    public byte getScale() {
        return scale;
    }
}
