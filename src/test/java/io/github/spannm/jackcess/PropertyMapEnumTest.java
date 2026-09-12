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

import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.PropertyMap.EnumValue;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the enum value constants declared in {@link PropertyMap}.
 */
class PropertyMapEnumTest extends AbstractBaseTest {

    @Test
    void enumValues() {
        List<EnumValue[]> allValues = List.of(
            PropertyMap.DisplayControl.values(),
            PropertyMap.TextFormat.values(),
            PropertyMap.IMEMode.values(),
            PropertyMap.IMESentenceMode.values());

        for (EnumValue[] values : allValues) {
            assertThat(values.length > 0).isTrue();
            for (EnumValue ev : values) {
                assertThat(ev.getValue()).isNotNull();
                String name = ((Enum<?>) ev).name();
                assertThat(ev.toString()).isEqualTo(name + "[" + ev.getValue() + "]");
            }
        }
    }

    @Test
    void specificEnumValues() {
        assertThat(PropertyMap.TextFormat.HTMLRICHTEXT.getValue()).isEqualTo(Byte.valueOf((byte) 1));
        assertThat(PropertyMap.TextFormat.PLAIN.getValue()).isEqualTo(Byte.valueOf((byte) 0));
        assertThat(PropertyMap.IMEMode.NOCONTROL.getValue()).isEqualTo(Byte.valueOf((byte) 0));
        assertThat(PropertyMap.IMEMode.HANGUL.getValue()).isEqualTo(Byte.valueOf((byte) 10));
        assertThat(PropertyMap.IMESentenceMode.NONE.getValue()).isEqualTo(Byte.valueOf((byte) 3));
        assertThat(PropertyMap.DisplayControl.RECTANGLE.getValue()).isEqualTo(Short.valueOf((short) 101));
    }

}
