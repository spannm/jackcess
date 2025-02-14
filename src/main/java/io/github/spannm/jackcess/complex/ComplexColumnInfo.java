package io.github.spannm.jackcess.complex;

import io.github.spannm.jackcess.Row;

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

    List<Row> getRawValues(int complexValueFk) throws IOException;

    List<Row> getRawValues(int complexValueFk,
        Collection<String> columnNames) throws IOException;

    List<V> getValues(ComplexValueForeignKey complexValueFk) throws IOException;

    ComplexValue.Id addRawValue(Map<String, ?> rawValue) throws IOException;

    ComplexValue.Id addValue(V value) throws IOException;

    void addValues(Collection<? extends V> values) throws IOException;

    ComplexValue.Id updateRawValue(Row rawValue) throws IOException;

    ComplexValue.Id updateValue(V value) throws IOException;

    void updateValues(Collection<? extends V> values) throws IOException;

    void deleteRawValue(Row rawValue) throws IOException;

    void deleteValue(V value) throws IOException;

    void deleteValues(Collection<? extends V> values) throws IOException;

    void deleteAllValues(int complexValueFk) throws IOException;

    void deleteAllValues(ComplexValueForeignKey complexValueFk) throws IOException;

}
