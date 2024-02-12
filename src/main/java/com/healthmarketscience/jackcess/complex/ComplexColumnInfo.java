/*
Copyright (c) 2013 James Ahlborn

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

package com.healthmarketscience.jackcess.complex;

import com.healthmarketscience.jackcess.Row;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base class for the additional information tracked for complex columns.
 *
 * @author James Ahlborn
 */
public interface ComplexColumnInfo<V extends ComplexValue> {
    ComplexDataType getType();

    int countValues(int complexValueFk) throws IOException;

    List<Row> getRawValues(int complexValueFk)
        throws IOException;

    List<Row> getRawValues(int complexValueFk,
        Collection<String> columnNames)
        throws IOException;

    List<V> getValues(ComplexValueForeignKey complexValueFk)
        throws IOException;

    ComplexValue.Id addRawValue(Map<String, ?> rawValue)
        throws IOException;

    ComplexValue.Id addValue(V value) throws IOException;

    void addValues(Collection<? extends V> values) throws IOException;

    ComplexValue.Id updateRawValue(Row rawValue) throws IOException;

    ComplexValue.Id updateValue(V value) throws IOException;

    void updateValues(Collection<? extends V> values) throws IOException;

    void deleteRawValue(Row rawValue) throws IOException;

    void deleteValue(V value) throws IOException;

    void deleteValues(Collection<? extends V> values) throws IOException;

    void deleteAllValues(int complexValueFk) throws IOException;

    void deleteAllValues(ComplexValueForeignKey complexValueFk)
        throws IOException;

}
