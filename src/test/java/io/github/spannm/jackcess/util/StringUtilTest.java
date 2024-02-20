package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

public class StringUtilTest extends AbstractBaseTest {

    @Test
    void testLength() {
        assertEquals(0, StringUtil.length(null));
        assertEquals(0, StringUtil.length(""));
        assertEquals(1, StringUtil.length("A"));
        assertEquals(1, StringUtil.length(" "));
        assertEquals(4, StringUtil.length("sman"));
    }

    @Test
    void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertFalse(StringUtil.isEmpty(" "));
        assertFalse(StringUtil.isEmpty("not Empty"));
    }

    @Test
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank(System.lineSeparator()));
    }

    @Test
    void testTrimToNull() {
        assertNull(StringUtil.trimToNull(null));
        assertNull(StringUtil.trimToNull(""));
        assertNull(StringUtil.trimToNull("   "));
        assertEquals("sman", StringUtil.trimToNull("sman"));
        assertEquals("81", StringUtil.trimToNull(" 81 "));
    }

    @Test
    void testCapitalize() {
        assertNull(StringUtil.capitalize(null));
        assertEquals("", StringUtil.capitalize(""));
        assertEquals("Hello", StringUtil.capitalize("hello"));
        assertEquals("Foo bar", StringUtil.capitalize("foo bar"));
        assertEquals("Boo far", StringUtil.capitalize("Boo far"));
    }

    @Test
    void testReplace() {
        assertNull(StringUtil.replace(null, null, null));
        assertEquals(" ", StringUtil.replace(" ", " ", " "));
        assertEquals("text", StringUtil.replace("text", "", "newText"));
        assertEquals(" txt txt ", StringUtil.replace(" text text ", "text", "txt"));
    }

    @Test
    void testRemove() {
        assertNull(StringUtil.remove(null, null));
        assertNull(StringUtil.remove(null, ""));
        assertNull(StringUtil.remove(null, "remove"));
        assertEquals("", StringUtil.remove("", "remove"));
        assertEquals("input", StringUtil.remove("input", "remove"));
        assertEquals("Removed", StringUtil.remove("Removed", "remove"));
        assertEquals("", StringUtil.remove("remove", "remove"));
        assertEquals("long", StringUtil.remove("long", "longer"));
    }

}
