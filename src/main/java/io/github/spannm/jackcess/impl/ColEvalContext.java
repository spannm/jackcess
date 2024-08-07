/*
Copyright (c) 2018 James Ahlborn

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

package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.expr.Value;

public abstract class ColEvalContext extends BaseEvalContext {
    private final ColumnImpl _col;

    public ColEvalContext(ColumnImpl col) {
        super(col.getDatabase().getEvalContext());
        _col = col;
    }

    protected ColumnImpl getCol() {
        return _col;
    }

    @Override
    protected String withErrorContext(String msg) {
        return _col.withErrorContext(msg);
    }

    protected Value toValue(Object val) {
        return toValue(val, _col.getType());
    }
}
