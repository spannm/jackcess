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

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tests for {@link MultiValueColumnPropertyMap}.
 */
class MultiValueColumnPropertyMapTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMPLEX_DATA)
    void testMultiValuePropertyMap(TestDb testDb) throws IOException {
        try (Database db = testDb.openCopy()) {
            Table t1 = db.getTable("Table1");
            Column col = t1.getColumn("multi-value-data");
            PropertyMap props = col.getProperties();
            assertInstanceOf(MultiValueColumnPropertyMap.class, props);

            assertNotNull(props.getName());
            assertFalse(props.isEmpty());
            assertTrue(props.getSize() > 1);

            assertEquals(Boolean.TRUE, props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP));
            assertNotNull(props.get(PropertyMap.ROW_SOURCE_TYPE_PROP));
            assertNull(props.get("bogusProperty"));
            assertNull(props.getValue("bogusProperty"));
            assertEquals("dflt", props.getValue("bogusProperty", "dflt"));

            int size = props.getSize();
            assertEquals(size, props.stream().count());

            // "put" of the multi-value property goes to the primary map, everything else to the complex map
            props.put(PropertyMap.ALLOW_MULTI_VALUE_PROP, Boolean.TRUE);
            props.put("newTextProp", "newVal");
            props.put("newLongProp", DataType.LONG, 42);
            assertEquals("newVal", props.getValue("newTextProp"));
            assertEquals(42, props.getValue("newLongProp"));

            props.putAll(null);
            props.putAll(List.of(props.get(PropertyMap.ALLOW_MULTI_VALUE_PROP), props.get("newTextProp")));
            assertEquals(Boolean.TRUE, props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP));

            props.save();

            assertNotNull(props.toString());

            // iterate the combined view
            int count = 0;
            Iterator<PropertyMap.Property> iter = props.iterator();
            while (iter.hasNext()) {
                assertNotNull(iter.next());
                count++;
            }
            assertEquals(props.getSize(), count);
            assertThrows(NoSuchElementException.class, iter::next);

            // remove via iterator
            Iterator<PropertyMap.Property> iter2 = props.iterator();
            assertTrue(iter2.hasNext());
            iter2.next();
            iter2.remove();
            iter2.remove();
            assertEquals(count - 1, props.getSize());

            assertNotNull(props.remove("newTextProp"));
            assertNull(props.remove("bogusProperty"));
            assertNotNull(props.remove(PropertyMap.ALLOW_MULTI_VALUE_PROP));
            assertNull(props.getValue(PropertyMap.ALLOW_MULTI_VALUE_PROP));
        }
    }

}
