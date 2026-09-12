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
package io.github.spannm.jackcess;

import io.github.spannm.jackcess.PropertyMap.EnumValue;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the enum value constants declared in {@link PropertyMap}.
 */
class PropertyMapEnumTest extends AbstractBaseTest {

    @Test
    void testEnumValues() {
        List<EnumValue[]> allValues = List.of(
            PropertyMap.DisplayControl.values(),
            PropertyMap.TextFormat.values(),
            PropertyMap.IMEMode.values(),
            PropertyMap.IMESentenceMode.values());

        for (EnumValue[] values : allValues) {
            assertTrue(values.length > 0);
            for (EnumValue ev : values) {
                assertNotNull(ev.getValue());
                String name = ((Enum<?>) ev).name();
                assertEquals(name + "[" + ev.getValue() + "]", ev.toString());
            }
        }
    }

    @Test
    void testSpecificEnumValues() {
        assertEquals(Byte.valueOf((byte) 1), PropertyMap.TextFormat.HTMLRICHTEXT.getValue());
        assertEquals(Byte.valueOf((byte) 0), PropertyMap.TextFormat.PLAIN.getValue());
        assertEquals(Byte.valueOf((byte) 0), PropertyMap.IMEMode.NOCONTROL.getValue());
        assertEquals(Byte.valueOf((byte) 10), PropertyMap.IMEMode.HANGUL.getValue());
        assertEquals(Byte.valueOf((byte) 3), PropertyMap.IMESentenceMode.NONE.getValue());
        assertEquals(Short.valueOf((short) 101), PropertyMap.DisplayControl.RECTANGLE.getValue());
    }

}
