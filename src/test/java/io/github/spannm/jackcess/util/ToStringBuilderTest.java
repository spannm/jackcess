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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

@DisplayName("ToStringBuilder")
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
                () -> assertTrue(result.startsWith("SampleObject["), "Should start with class name + '['"),
                () -> assertFalse(result.contains("@"), "No identity hash code for valueBuilder"),
                () -> assertTrue(result.contains("field=value"), "Field name and value must be present")
            );
        }

        @Test
        @DisplayName("Null object returns '<null>'")
        void nullObject_returnsNullText() {
            String result = ToStringBuilder.valueBuilder(null).toString();
            assertEquals("<null>", result);
        }

        @Test
        @DisplayName("String object is used directly as label prefix")
        void stringObject_usedDirectly() {
            String result = ToStringBuilder.valueBuilder("MyLabel")
                .append(null, 42)
                .toString();
            assertTrue(result.startsWith("MyLabel["), "String object should be used directly as prefix");
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

            assertTrue(result.matches("(?s)SampleObject@[0-9a-f]+\\[.*"), "Should contain '@<hex>'");
        }

        @Test
        @DisplayName("'Impl' suffix is stripped from class name")
        void implSuffix_isStripped() {
            String result = ToStringBuilder.builder(new SampleObjectImpl())
                .append("a", "b")
                .toString();
            assertTrue(result.startsWith("SampleObject@"), "'Impl' suffix should be removed");
        }

        @Test
        @DisplayName("Multiple fields are separated correctly")
        void multipleFields_separatedCorrectly() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("foo", "bar")
                .append("num", 42)
                .toString();

            assertAll(
                () -> assertTrue(result.contains("foo: bar"), "First field"),
                () -> assertTrue(result.contains("num: 42"), "Second field")
            );
        }

        @Test
        @DisplayName("Null value is rendered as '<null>'")
        void nullValue_renderedAsNullText() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("key", null)
                .toString();
            assertTrue(result.contains("<null>"), "null should appear as '<null>'");
        }

        @Test
        @DisplayName("No trailing field separator before contentEnd")
        void noTrailingFieldSeparator() {
            String result = ToStringBuilder.builder(new SampleObject())
                .append("only", "field")
                .toString();
            assertFalse(result.endsWith("," + System.lineSeparator() + "]"),
                "No trailing separator expected");
            assertTrue(result.endsWith(System.lineSeparator() + "]"),
                "Should end with newline + ']'");
        }
    }

    @Nested
    @DisplayName("append() – type handling")
    class AppendTypeTests {

        @Test
        @DisplayName("int value")
        void appendInt() {
            String result = ToStringBuilder.valueBuilder("T").append("n", 7).toString();
            assertTrue(result.contains("n=7"));
        }

        @Test
        @DisplayName("boolean value")
        void appendBoolean() {
            String result = ToStringBuilder.valueBuilder("T").append("flag", true).toString();
            assertTrue(result.contains("flag=true"));
        }

        @Test
        @DisplayName("String array")
        void appendStringArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{"a", "b", "c"})
                .toString();
            assertAll(
                () -> assertTrue(result.contains("{"), "Array opening '{'"),
                () -> assertTrue(result.contains("a"), "First element"),
                () -> assertTrue(result.contains("c"), "Last element"),
                () -> assertTrue(result.contains("}"), "Array closing '}'")
            );
        }

        @Test
        @DisplayName("Array with null elements")
        void appendArrayWithNullElements() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{"x", null, "z"})
                .toString();
            assertTrue(result.contains("<null>"), "Null element should appear as '<null>'");
        }

        @Test
        @DisplayName("Empty array")
        void appendEmptyArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("arr", new String[]{})
                .toString();
            assertTrue(result.contains("{}"), "Empty array should appear as '{}'");
        }

        @Test
        @DisplayName("Collection")
        void appendCollection() {
            List<String> list = Arrays.asList("x", "y");
            String result = ToStringBuilder.valueBuilder("T").append("list", list).toString();
            assertAll(
                () -> assertTrue(result.contains("["), "Collection wrapped in '['"),
                () -> assertTrue(result.contains("x"), "Element x"),
                () -> assertTrue(result.contains("y"), "Element y")
            );
        }

        @Test
        @DisplayName("Collection with null element")
        void appendCollectionWithNull() {
            List<String> list = new ArrayList<>();
            list.add("a");
            list.add(null);
            String result = ToStringBuilder.valueBuilder("T").append("list", list).toString();
            assertTrue(result.contains("<null>"), "null in Collection rendered as '<null>'");
        }

        @Test
        @DisplayName("Map")
        void appendMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("k1", "v1");
            map.put("k2", "v2");
            String result = ToStringBuilder.valueBuilder("T").append("map", map).toString();
            assertAll(
                () -> assertTrue(result.contains("k1=v1")),
                () -> assertTrue(result.contains("k2=v2"))
            );
        }

        @Test
        @DisplayName("Map with null value")
        void appendMapWithNullValue() {
            Map<String, String> map = new HashMap<>();
            map.put("key", null);
            String result = ToStringBuilder.valueBuilder("T").append("map", map).toString();
            assertTrue(result.contains("key=<null>"), "null value in Map rendered as '<null>'");
        }

        @Test
        @DisplayName("Primitive int array")
        void appendPrimitiveIntArray() {
            String result = ToStringBuilder.valueBuilder("T")
                .append("nums", new int[]{1, 2, 3})
                .toString();
            assertAll(
                () -> assertTrue(result.contains("{"), "Array braces present"),
                () -> assertTrue(result.contains("1")),
                () -> assertTrue(result.contains("3"))
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
            assertTrue(result.contains("field=hello"));
        }

        @Test
        @DisplayName("Null value is silently skipped – field does not appear in output")
        void nullValue_isSkipped() {
            String result = ToStringBuilder.valueBuilder("T")
                .appendIgnoreNull("skip", null)
                .append("keep", "yes")
                .toString();
            assertAll(
                () -> assertFalse(result.contains("skip"), "Null field must not appear"),
                () -> assertTrue(result.contains("keep=yes"), "Non-null field must appear"),
                () -> assertFalse(result.contains("<null>"), "No '<null>' for appendIgnoreNull")
            );
        }

        @Test
        @DisplayName("Only null fields via appendIgnoreNull – content is empty")
        void onlyNullFields_emptyContent() {
            String result = ToStringBuilder.valueBuilder("T")
                .appendIgnoreNull("a", null)
                .appendIgnoreNull("b", null)
                .toString();
            assertTrue(result.startsWith("T["), "Prefix correct");
            assertFalse(result.contains("="), "No fields in output");
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
            assertEquals("Foo", ToStringBuilder.getShortClassName(FooImpl.class, "Impl"));
        }

        @Test
        @DisplayName("No 'Impl' suffix – name is unchanged")
        void noImplSuffix_unchanged() {
            assertEquals("SampleObject", ToStringBuilder.getShortClassName(SampleObject.class, "Impl"));
        }

        @Test
        @DisplayName("Empty suffix – nothing is stripped")
        void emptySuffix_noChange() {
            assertEquals("SampleObjectImpl", ToStringBuilder.getShortClassName(SampleObjectImpl.class, ""));
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
            assertEquals("hello", sb.toString());
        }

        @Test
        @DisplayName("No trailing separator – buffer unchanged")
        void noSeparatorAtEnd_unchanged() {
            StringBuilder sb = new StringBuilder("hello");
            ToStringBuilder.removeLastFieldSeparator(sb, ",");
            assertEquals("hello", sb.toString());
        }

        @Test
        @DisplayName("Multi-character separator is removed")
        void multiCharSeparator_removed() {
            StringBuilder sb = new StringBuilder("abc\n  ");
            ToStringBuilder.removeLastFieldSeparator(sb, "\n  ");
            assertEquals("abc", sb.toString());
        }

        @Test
        @DisplayName("Empty separator – buffer unchanged")
        void emptySeparator_noChange() {
            StringBuilder sb = new StringBuilder("data");
            ToStringBuilder.removeLastFieldSeparator(sb, "");
            assertEquals("data", sb.toString());
        }

        @Test
        @DisplayName("Empty buffer – no exception")
        void emptyBuffer_noException() {
            StringBuilder sb = new StringBuilder();
            assertDoesNotThrow(() -> ToStringBuilder.removeLastFieldSeparator(sb, ","));
            assertEquals("", sb.toString());
        }

        @Test
        @DisplayName("Separator longer than buffer – buffer unchanged")
        void separatorLongerThanBuffer_noChange() {
            StringBuilder sb = new StringBuilder("ab");
            ToStringBuilder.removeLastFieldSeparator(sb, "abc");
            assertEquals("ab", sb.toString());
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTests {

        @Test
        @DisplayName("append() returns same builder instance (chaining)")
        void append_returnsSameBuilder() {
            ToStringBuilder builder = ToStringBuilder.valueBuilder("T");
            assertSame(builder, builder.append("a", 1), "Method should return 'this'");
        }

        @Test
        @DisplayName("appendIgnoreNull() returns same builder instance")
        void appendIgnoreNull_returnsSameBuilder() {
            ToStringBuilder builder = ToStringBuilder.valueBuilder("T");
            assertSame(builder, builder.appendIgnoreNull("a", null));
        }

        @Test
        @DisplayName("Null field name – only value is appended, no '=' prefix")
        void nullFieldName_onlyValueAppended() {
            String result = ToStringBuilder.valueBuilder("T").append(null, "justValue").toString();
            assertTrue(result.contains("justValue"), "Value must be present");
            assertFalse(result.contains("=justValue"), "No '=' without field name");
        }
    }

}
