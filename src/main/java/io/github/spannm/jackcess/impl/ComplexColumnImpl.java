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
 *
 * @author James Ahlborn
 */
class ComplexColumnImpl extends ColumnImpl {
    /** additional information specific to complex columns */
    private final ComplexColumnInfo<? extends ComplexValue> _complexInfo;
    /** properties for multi-value column */
    private PropertyMap                                     _mvProps;

    ComplexColumnImpl(InitArgs args) throws IOException {
        super(args);
        _complexInfo = ComplexColumnSupport.create(this, args.buffer, args.offset);
    }

    @Override
    void postTableLoadInit() throws IOException {
        if (_complexInfo != null) {
            ((ComplexColumnInfoImpl<? extends ComplexValue>) _complexInfo)
                .postTableLoadInit();
        }
        super.postTableLoadInit();
    }

    @Override
    public PropertyMap getProperties() throws IOException {
        if (_complexInfo.getType() == ComplexDataType.MULTI_VALUE) {
            if (_mvProps == null) {
                PropertyMap primaryProps = super.getProperties();
                PropertyMap complexProps = ((MultiValueColumnInfoImpl) _complexInfo)
                    .getValueColumn().getProperties();
                _mvProps = new MultiValueColumnPropertyMap(primaryProps, complexProps);
            }
            return _mvProps;
        }
        return super.getProperties();
    }

    @Override
    public ComplexColumnInfo<? extends ComplexValue> getComplexInfo() {
        return _complexInfo;
    }
}
