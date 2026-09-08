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

import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.complex.ComplexColumnInfo;
import io.github.spannm.jackcess.complex.ComplexDataType;
import io.github.spannm.jackcess.complex.ComplexValue;
import io.github.spannm.jackcess.impl.complex.ComplexColumnInfoImpl;
import io.github.spannm.jackcess.impl.complex.MultiValueColumnInfoImpl;
import io.github.spannm.jackcess.impl.complex.MultiValueColumnPropertyMap;

import java.io.IOException;

/**
 * ColumnImpl subclass which is used for complex data types.
 */
class ComplexColumnImpl extends ColumnImpl {
    /** additional information specific to complex columns */
    private final ComplexColumnInfo<? extends ComplexValue> complexInfo;
    /** properties for multi-value column */
    private PropertyMap                                     mvProps;

    ComplexColumnImpl(InitArgs args) throws IOException {
        super(args);
        complexInfo = ComplexColumnSupport.create(this, args.buffer, args.offset);
    }

    @Override
    void postTableLoadInit() throws IOException {
        if (complexInfo != null) {
            ((ComplexColumnInfoImpl<? extends ComplexValue>) complexInfo).postTableLoadInit();
        }
        super.postTableLoadInit();
    }

    @Override
    public PropertyMap getProperties() throws IOException {
        if (complexInfo.getType() == ComplexDataType.MULTI_VALUE) {
            if (mvProps == null) {
                PropertyMap primaryProps = super.getProperties();
                PropertyMap complexProps = ((MultiValueColumnInfoImpl) complexInfo).getValueColumn().getProperties();
                mvProps = new MultiValueColumnPropertyMap(primaryProps, complexProps);
            }
            return mvProps;
        }
        return super.getProperties();
    }

    @Override
    public ComplexColumnInfo<? extends ComplexValue> getComplexInfo() {
        return complexInfo;
    }
}
