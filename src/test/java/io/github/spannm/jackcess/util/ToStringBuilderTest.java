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
/*
 * JUnit 5 tests for ToStringBuilder
 */
package io.github.spannm.jackcess.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

@SuppressWarnings("checkstyle:MethodName")
class ToStringBuilderTest {

    // Helper types  (must be static top-level or static members of the
    // outermost class; non-static nested @Nested classes cannot own static types)

    static class SampleObject {}

    static class SampleObjectImpl {}

    static class FooImpl {}

    @Nested
    @DisplayName("valueBuilder()")
    class ValueBuilderTests {

        @Test
        @DisplayName("Simple object – no identity hash code, no leading separator")
        void simpleObject_noHashCode() {
            String result = ToStringBuilder.valueBuilder(new SampleObject())
                .append("field", "value")
                .toString();

            assertAll(
                () -> assertThat(result.startsWith("SampleObject[")).as("Should start with class name + '['").isTrue(),
                () -> assertThat(result.contains("@")).as("No identity hash code for valueBuilder").isFalse(),
                () -> assertThat(result.contains("field=value")).as("Field name and value must be present").isTrue()
            );
        }

        @Test
        @DisplayName("Null object returns '<null>'")
        void nullObject_returnsNullText() {
            String result = ToStringBuilder.valueBuilder(null).toString();
            assertThat(result).isEqualTo("<null>");
        }

        @Test
        @DisplayName("String object is used directly as label prefix")
        void stringObject_usedDirectly() {
            String result = ToStringBuilder.valueBuilder("MyLabel")
                .append(null, 42)
                .toString();
            assertThat(result.startsWith("MyLabel[")).as("String object should be used directly as prefix").isTrue();
        }
    }

    @Nested
    @DisplayName("builder()")
    class BuilderTests {

        @Test
        @DisplayName("Contains identity hash code (@hex)")
        void containsIdentityHashCode() {
            Object obj = new SampleObject();
            String result = ToStringBuilder.builder(obj)
                .append("x", 1)
                .toString();

            assertThat(result.matches("(?s)SampleObject@[0-9a-f]+\\[.*")).as("Should contain '@<hex>'").isTrue();
        }

        @Test
        @DisplayName("'Impl' suffix is stripped from class name")
        void implSuffix_isStripped() {
            String result = ToStringBuilder.builder(new SampleObjectImpl())
                .append("a", "b")
                .toString();
            assertThat(result.startsWith("SampleObject@")).as("'Impl' suffix should be removed").isTrue();
        }

        @Test
        @DisplayName("Multiple fields are separated correctly")
        void multipleFields_separatedCorrectly() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("foo", "bar")
                .append("num", 42)
                .toString();

            assertAll(
                () -> assertThat(result.contains("foo: bar")).as("First field").isTrue(),
                () -> assertThat(result.contains("num: 42")).as("Second field").isTrue()
            );
        }

        @Test
        @DisplayName("Null value is rendered as '<null>'")
        void nullValue_renderedAsNullText() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("key", null)
                .toString();
            assertThat(result.contains("<null>")).as("null should appear as '<null>'").isTrue();
        }

        @Test
        @DisplayName("No trailing field separator before contentEnd")
        void noTrailingFieldSeparator() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("only", "field")
                .toString();
            assertThat(result.endsWith("," + System.lineSeparator() + "]")).as("No trailing separator expected").isFalse();
            assertThat(result.endsWith(System.lineSeparator() + "]")).as("Should end with newline + ']'").isTrue();
        }
    }

    @Nested
    @DisplayName("append() – type handling")
    class AppendTypeTests {

        @Test
        @DisplayName("int value")
        void appendInt() {
            String result = ToStringBuilder.valueBuilder("T").append("n", 7).toString();
            assertThat(result.contains("n=7")).isTrue();
        }

        @Test
        @DisplayName("boolean value")
        void appendBoolean() {
            String result = ToStringBuilder.valueBuilder("T").append("flag", true).toString();
            assertThat(result.contains("flag=true")).isTrue();
        }

        @Test
        @DisplayName("String array")
        void appendStringArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{"a", "b", "c"})
                .toString();
            assertAll(
                () -> assertThat(result.contains("{")).as("Array opening '{'").isTrue(),
                () -> assertThat(result.contains("a")).as("First element").isTrue(),
                () -> assertThat(result.contains("c")).as("Last element").isTrue(),
                () -> assertThat(result.contains("}")).as("Array closing '}'").isTrue()
            );
        }

        @Test
        @DisplayName("Array with null elements")
        void appendArrayWithNullElements() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{"x", null, "z"})
                .toString();
            assertThat(result.contains("<null>")).as("Null element should appear as '<null>'").isTrue();
        }

        @Test
        @DisplayName("Empty array")
        void appendEmptyArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{})
                .toString();
            assertThat(result.contains("{}")).as("Empty array should appear as '{}'").isTrue();
        }

        @Test
        @DisplayName("Collection")
        void appendCollection() {
            List<String> list = Arrays.asList("x", "y");
            String result = ToStringBuilder.valueBuilder("T").append("list", list).toString();
            assertAll(
                () -> assertThat(result.contains("[")).as("Collection wrapped in '['").isTrue(),
                () -> assertThat(result.contains("x")).as("Element x").isTrue(),
                () -> assertThat(result.contains("y")).as("Element y").isTrue()
            );
        }

        @Test
        @DisplayName("Collection with null element")
        void appendCollectionWithNull() {
            List<String> list = new ArrayList<>();
            list.add("a");
            list.add(null);
            String result = ToStringBuilder.valueBuilder("T").append("list", list).toString();
            assertThat(result.contains("<null>")).as("null in Collection rendered as '<null>'").isTrue();
        }

        @Test
        @DisplayName("Map")
        void appendMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("k1", "v1");
            map.put("k2", "v2");
            String result = ToStringBuilder.valueBuilder("T").append("map", map).toString();
            assertAll(
                () -> assertThat(result.contains("k1=v1")).isTrue(),
                () -> assertThat(result.contains("k2=v2")).isTrue()
            );
        }

        @Test
        @DisplayName("Map with null value")
        void appendMapWithNullValue() {
            Map<String, String> map = new HashMap<>();
            map.put("key", null);
            String result = ToStringBuilder.valueBuilder("T").append("map", map).toString();
            assertThat(result.contains("key=<null>")).as("null value in Map rendered as '<null>'").isTrue();
        }

        @Test
        @DisplayName("Primitive int array")
        void appendPrimitiveIntArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("nums", new int[]{1, 2, 3})
                .toString();
            assertAll(
                () -> assertThat(result.contains("{")).as("Array braces present").isTrue(),
                () -> assertThat(result.contains("1")).isTrue(),
                () -> assertThat(result.contains("3")).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("appendIgnoreNull()")
    class AppendIgnoreNullTests {

        @Test
        @DisplayName("Non-null value is appended normally")
        void nonNullValue_appendedNormally() {
            String result = ToStringBuilder.valueBuilder("T")
                .appendIgnoreNull("field", "hello")
                .toString();
            assertThat(result.contains("field=hello")).isTrue();
        }

        @Test
        @DisplayName("Null value is silently skipped – field does not appear in output")
        void nullValue_isSkipped() {
            String result = ToStringBuilder.valueBuilder("T")
                .appendIgnoreNull("skip", null)
                .append("keep", "yes")
                .toString();
            assertAll(
                () -> assertThat(result.contains("skip")).as("Null field must not appear").isFalse(),
                () -> assertThat(result.contains("keep=yes")).as("Non-null field must appear").isTrue(),
                () -> assertThat(result.contains("<null>")).as("No '<null>' for appendIgnoreNull").isFalse()
            );
        }

        @Test
        @DisplayName("Only null fields via appendIgnoreNull – content is empty")
        void onlyNullFields_emptyContent() {
            String result = ToStringBuilder.valueBuilder("T")
                .appendIgnoreNull("a", null)
                .appendIgnoreNull("b", null)
                .toString();
            assertThat(result.startsWith("T[")).as("Prefix correct").isTrue();
            assertThat(result.contains("=")).as("No fields in output").isFalse();
        }
    }

    @Nested
    @DisplayName("Cycle detection (OBJ_REGISTRY)")
    class CycleDetectionTests {

        @Test
        @DisplayName("Self-referencing collection does not cause StackOverflowError")
        void selfReferencingCollection_noCycleException() {
            // The list contains itself – registry must detect the cycle via reference
            // equality (==) without calling hashCode(), which would itself overflow.
            List<Object> circular = new ArrayList<>();
            circular.add(circular);

            assertDoesNotThrow(
                () -> ToStringBuilder.valueBuilder("T").append("self", circular).toString(),
                "Cycle detection should prevent StackOverflowError"
            );
        }
    }

    @Nested
    @DisplayName("getShortClassName()")
    class GetShortClassNameTests {

        @Test
        @DisplayName("'Impl' suffix is removed")
        void implSuffix_removed() {
            assertThat(ToStringBuilder.getShortClassName(FooImpl.class, "Impl")).isEqualTo("Foo");
        }

        @Test
        @DisplayName("No 'Impl' suffix – name is unchanged")
        void noImplSuffix_unchanged() {
            assertThat(ToStringBuilder.getShortClassName(SampleObject.class, "Impl")).isEqualTo("SampleObject");
        }

        @Test
        @DisplayName("Empty suffix – nothing is stripped")
        void emptySuffix_noChange() {
            assertThat(ToStringBuilder.getShortClassName(SampleObjectImpl.class, "")).isEqualTo("SampleObjectImpl");
        }
    }

    @Nested
    @DisplayName("removeLastFieldSeparator()")
    class RemoveLastFieldSeparatorTests {

        @Test
        @DisplayName("Trailing separator is removed")
        void separatorAtEnd_removed() {
            StringBuilder sb = new StringBuilder("hello,");
            ToStringBuilder.removeLastFieldSeparator(sb, ",");
            assertThat(sb.toString()).isEqualTo("hello");
        }

        @Test
        @DisplayName("No trailing separator – buffer unchanged")
        void noSeparatorAtEnd_unchanged() {
            StringBuilder sb = new StringBuilder("hello");
            ToStringBuilder.removeLastFieldSeparator(sb, ",");
            assertThat(sb.toString()).isEqualTo("hello");
        }

        @Test
        @DisplayName("Multi-character separator is removed")
        void multiCharSeparator_removed() {
            StringBuilder sb = new StringBuilder("abc\n  ");
            ToStringBuilder.removeLastFieldSeparator(sb, "\n  ");
            assertThat(sb.toString()).isEqualTo("abc");
        }

        @Test
        @DisplayName("Empty separator – buffer unchanged")
        void emptySeparator_noChange() {
            StringBuilder sb = new StringBuilder("data");
            ToStringBuilder.removeLastFieldSeparator(sb, "");
            assertThat(sb.toString()).isEqualTo("data");
        }

        @Test
        @DisplayName("Empty buffer – no exception")
        void emptyBuffer_noException() {
            StringBuilder sb = new StringBuilder();
            assertDoesNotThrow(() -> ToStringBuilder.removeLastFieldSeparator(sb, ","));
            assertThat(sb.toString()).isEqualTo("");
        }

        @Test
        @DisplayName("Separator longer than buffer – buffer unchanged")
        void separatorLongerThanBuffer_noChange() {
            StringBuilder sb = new StringBuilder("ab");
            ToStringBuilder.removeLastFieldSeparator(sb, "abc");
            assertThat(sb.toString()).isEqualTo("ab");
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTests {

        @Test
        @DisplayName("append() returns same builder instance (chaining)")
        void append_returnsSameBuilder() {
            ToStringBuilder builder = ToStringBuilder.valueBuilder("T");
            assertThat(builder.append("a", 1)).as("Method should return 'this'").isSameAs(builder);
        }

        @Test
        @DisplayName("appendIgnoreNull() returns same builder instance")
        void appendIgnoreNull_returnsSameBuilder() {
            ToStringBuilder builder = ToStringBuilder.valueBuilder("T");
            assertThat(builder.appendIgnoreNull("a", null)).isSameAs(builder);
        }

        @Test
        @DisplayName("Null field name – only value is appended, no '=' prefix")
        void nullFieldName_onlyValueAppended() {
            String result = ToStringBuilder.valueBuilder("T").append(null, "justValue").toString();
            assertThat(result.contains("justValue")).as("Value must be present").isTrue();
            assertThat(result.contains("=justValue")).as("No '=' without field name").isFalse();
        }
    }

}
