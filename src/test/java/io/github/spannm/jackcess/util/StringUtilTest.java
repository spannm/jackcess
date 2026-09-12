/*
 * Copyright (C) 2024- Markus Spann
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
package io.github.spannm.jackcess.util;

import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

class StringUtilTest extends AbstractBaseTest {

    @Test
    void length() {
        assertThat(StringUtil.length(null)).isEqualTo(0);
        assertThat(StringUtil.length("")).isEqualTo(0);
        assertThat(StringUtil.length("A")).isEqualTo(1);
        assertThat(StringUtil.length(" ")).isEqualTo(1);
        assertThat(StringUtil.length("sman")).isEqualTo(4);
    }

    @Test
    void isEmpty() {
        assertThat(StringUtil.isEmpty(null)).isTrue();
        assertThat(StringUtil.isEmpty("")).isTrue();
        assertThat(StringUtil.isEmpty(" ")).isFalse();
        assertThat(StringUtil.isEmpty("not Empty")).isFalse();
    }

    @Test
    void isBlank() {
        assertThat(StringUtil.isBlank(null)).isTrue();
        assertThat(StringUtil.isBlank("")).isTrue();
        assertThat(StringUtil.isBlank("   ")).isTrue();
        assertThat(StringUtil.isBlank(System.lineSeparator())).isTrue();
    }

    @Test
    void trimToNull() {
        assertThat(StringUtil.trimToNull(null)).isNull();
        assertThat(StringUtil.trimToNull("")).isNull();
        assertThat(StringUtil.trimToNull("   ")).isNull();
        assertThat(StringUtil.trimToNull("sman")).isEqualTo("sman");
        assertThat(StringUtil.trimToNull(" 81 ")).isEqualTo("81");
    }

    @Test
    void capitalize() {
        assertThat(StringUtil.capitalize(null)).isNull();
        assertThat(StringUtil.capitalize("")).isEqualTo("");
        assertThat(StringUtil.capitalize("hello")).isEqualTo("Hello");
        assertThat(StringUtil.capitalize("foo bar")).isEqualTo("Foo bar");
        assertThat(StringUtil.capitalize("Boo far")).isEqualTo("Boo far");
    }

    @Test
    void replace() {
        assertThat(StringUtil.replace(null, null, null)).isNull();
        assertThat(StringUtil.replace(" ", " ", " ")).isEqualTo(" ");
        assertThat(StringUtil.replace("text", "", "newText")).isEqualTo("text");
        assertThat(StringUtil.replace(" text text ", "text", "txt")).isEqualTo(" txt txt ");
    }

    @Test
    void remove() {
        assertThat(StringUtil.remove(null, null)).isNull();
        assertThat(StringUtil.remove(null, "")).isNull();
        assertThat(StringUtil.remove(null, "remove")).isNull();
        assertThat(StringUtil.remove("", "remove")).isEqualTo("");
        assertThat(StringUtil.remove("input", "remove")).isEqualTo("input");
        assertThat(StringUtil.remove("Removed", "remove")).isEqualTo("Removed");
        assertThat(StringUtil.remove("remove", "remove")).isEqualTo("");
        assertThat(StringUtil.remove("long", "longer")).isEqualTo("long");
    }

}
