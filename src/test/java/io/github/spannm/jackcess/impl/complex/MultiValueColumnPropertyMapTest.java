/*
 * Copyright (c) 2025 Markus Spann
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
package io.github.spannm.jackcess.impl.complex;

import static io.github.spannm.jackcess.test.Basename.COMPLEX_DATA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tests for {@link MultiValueColumnPropertyMap}.
 */
class MultiValueColumnPropertyMapTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void multiValuePropertyMap(TestDb testDb) throws Exception {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Table1");
            Column col = t1.getColumn("multi-value-data");
            PropertyMap props = col.getProperties();
            assertThat(props).isInstanceOf(MultiValueColumnPropertyMap.class);

            assertThat(props.getName()).isNotNull();
            assertThat(props.isEmpty()).isFalse();
            assertThat(props.getSize() > 1).isTrue();

            assertThat(props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP)).isEqualTo(Boolean.TRUE);
            assertThat(props.get(PropertyMap.ROW_SOURCE_TYPE_PROP)).isNotNull();
            assertThat(props.get("bogusProperty")).isNull();
            assertThat(props.getValue("bogusProperty")).isNull();
            assertThat(props.getValue("bogusProperty", "dflt")).isEqualTo("dflt");

            int size = props.getSize();
            assertThat(props.stream().count()).isEqualTo(size);

            // "put" of the multi-value property goes to the primary map, everything else to the complex map
            props.put(PropertyMap.ALLOW_MULTI_VALUE_PROP, Boolean.TRUE);
            props.put("newTextProp", "newVal");
            props.put("newLongProp", DataType.LONG, 42);
            assertThat(props.getValue("newTextProp")).isEqualTo("newVal");
            assertThat(props.getValue("newLongProp")).isEqualTo(42);

            props.putAll(null);
            props.putAll(List.of(props.get(PropertyMap.ALLOW_MULTI_VALUE_PROP), props.get("newTextProp")));
            assertThat(props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP)).isEqualTo(Boolean.TRUE);

            props.save();

            assertThat(props.toString()).isNotNull();

            // iterate the combined view
            int count = 0;
            Iterator<PropertyMap.Property> iter = props.iterator();
            while (iter.hasNext()) {
                assertThat(iter.next()).isNotNull();
                count++;
            }
            assertThat(count).isEqualTo(props.getSize());
            assertThrows(NoSuchElementException.class, iter::next);

            // remove via iterator
            Iterator<PropertyMap.Property> iter2 = props.iterator();
            assertThat(iter2.hasNext()).isTrue();
            iter2.next();
            iter2.remove();
            iter2.remove();
            assertThat(props.getSize()).isEqualTo(count - 1);

            assertThat(props.remove("newTextProp")).isNotNull();
            assertThat(props.remove("bogusProperty")).isNull();
            assertThat(props.remove(PropertyMap.ALLOW_MULTI_VALUE_PROP)).isNotNull();
            assertThat(props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP)).isNull();
        }
    }

}
