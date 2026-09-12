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

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests for {@link ColumnBuilder}.
 */
class ColumnBuilderTest extends AbstractBaseTest {

    @Test
    void testMaxPrecisionAndScale() {
        ColumnBuilder cb = new ColumnBuilder("num", DataType.NUMERIC)
            .withMaxPrecision().withMaxScale();
        assertEquals((byte) DataType.NUMERIC.getMaxPrecision(), cb.getPrecision());
        assertEquals((byte) DataType.NUMERIC.getMaxScale(), cb.getScale());

        // types without scale/precision are left untouched
        ColumnBuilder txt = new ColumnBuilder("txt", DataType.TEXT)
            .withMaxPrecision().withMaxScale();
        assertEquals((byte) DataType.TEXT.getDefaultPrecision(), txt.getPrecision());
        assertEquals((byte) DataType.TEXT.getDefaultScale(), txt.getScale());
    }

    @Test
    void testMaxLength() {
        ColumnBuilder txt = new ColumnBuilder("txt", DataType.TEXT).withMaxLength();
        assertEquals((short) DataType.TEXT.getMaxSize(), txt.getLength());

        // fixed length types are left untouched
        ColumnBuilder lng = new ColumnBuilder("lng", DataType.LONG).withMaxLength();
        assertEquals((short) DataType.LONG.getFixedSize(), lng.getLength());
        assertEquals(DataType.LONG.getFixedSize(), lng.getFixedDataSize());
    }

    @Test
    void testFlags() {
        ColumnBuilder cb = new ColumnBuilder("memo", DataType.MEMO)
            .withCompressedUnicode(true)
            .withHyperlink(true)
            .withAutoNumber(false);
        assertTrue(cb.isCompressedUnicode());
        assertTrue(cb.isHyperlink());
        assertFalse(cb.isAutoNumber());
        assertTrue(cb.isVariableLength());
        assertFalse(cb.storeInNullMask());
        assertTrue(new ColumnBuilder("b", DataType.BOOLEAN).storeInNullMask());

        cb.setColumnNumber((short) 3);
        assertEquals((short) 3, cb.getColumnNumber());
        cb.setTextSortOrder(ColumnImpl.GENERAL_SORT_ORDER);
        assertEquals(ColumnImpl.GENERAL_SORT_ORDER, cb.getTextSortOrder());
    }

    @Test
    void testProperties() {
        ColumnBuilder cb = new ColumnBuilder("c", DataType.TEXT);
        assertNull(cb.getProperties());
        cb.withProperty("myprop", "myval");
        assertNotNull(cb.getProperties());
        assertEquals("myval", cb.getProperties().get("myprop").getValue());
        cb.withProperty("intprop", DataType.LONG, 13);
        assertEquals(13, cb.getProperties().get("intprop").getValue());
    }

    @Test
    void testFromColumnBuilder() {
        ColumnBuilder template = new ColumnBuilder("tmpl", DataType.NUMERIC)
            .withScale(3).withPrecision(10)
            .withAutoNumber(false)
            .withCompressedUnicode(true)
            .withHyperlink(true)
            .withCalculated(false)
            .withProperty("aprop", "aval");
        template.setTextSortOrder(ColumnImpl.GENERAL_SORT_ORDER);

        ColumnBuilder copy = new ColumnBuilder("copy").withFromColumn(template);
        assertEquals(DataType.NUMERIC, copy.getType());
        assertEquals((byte) 3, copy.getScale());
        assertEquals((byte) 10, copy.getPrecision());
        assertTrue(copy.isCompressedUnicode());
        assertTrue(copy.isHyperlink());
        assertFalse(copy.isCalculated());
        assertEquals(ColumnImpl.GENERAL_SORT_ORDER, copy.getTextSortOrder());
        assertEquals("aval", copy.getProperties().get("aprop").getValue());
        assertEquals("copy", copy.getName());
    }

    @Test
    void testFromColumn() throws IOException {
        try (Database db = createDbMem(FileFormat.V2000)) {
            TestUtil.createTestTable(db);
            Table table = db.getTable("test");
            Column src = table.getColumns().iterator().next();

            ColumnBuilder cb = new ColumnBuilder("copy").withFromColumn(src);
            assertEquals(src.getType(), cb.getType());
            assertEquals(src.isAutoNumber(), cb.isAutoNumber());
            assertEquals(src.isCalculated(), cb.isCalculated());
            assertEquals(src.isHyperlink(), cb.isHyperlink());
        }
    }

    @Test
    void testSqlType() throws IOException {
        ColumnBuilder cb = new ColumnBuilder("c").withSqlType(java.sql.Types.INTEGER);
        assertEquals(DataType.LONG, cb.getType());
        cb = new ColumnBuilder("c").withSqlType(java.sql.Types.VARCHAR, 100);
        assertEquals(DataType.TEXT, cb.getType());
        cb = new ColumnBuilder("c").withSqlType(java.sql.Types.VARCHAR, 100, FileFormat.V2003);
        assertEquals(DataType.TEXT, cb.getType());
    }

    @Test
    void testValidate() {
        JetFormat fmt = JetFormat.VERSION_4;

        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c").validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.UNSUPPORTED_FIXEDLEN).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.BIG_INT).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.LONG).withLength(1).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.NUMERIC).withScale(99).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.NUMERIC).withScale(1).withPrecision(99).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.TEXT).withAutoNumber(true).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.LONG).withCompressedUnicode(true).validate(fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.TEXT).withHyperlink(true).validate(fmt));

        // valid definitions
        new ColumnBuilder("c", DataType.TEXT).withMaxLength().validate(fmt);
        new ColumnBuilder("c", DataType.MEMO).withHyperlink(true).validate(fmt);
        new ColumnBuilder("c", DataType.LONG).withAutoNumber(true).validate(fmt);
    }

    @Test
    void testValidateCalculated() {
        JetFormat fmt14 = JetFormat.VERSION_14;

        // calculated not supported in older format
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.LONG).withCalculated(true).validate(JetFormat.VERSION_4));

        // no expression
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnBuilder("c", DataType.LONG).withCalculated(true).validate(fmt14));

        ColumnBuilder cb = new ColumnBuilder("c", DataType.LONG)
            .withCalculated(true)
            .withProperty(PropertyMap.EXPRESSION_PROP, "[a]+[b]");
        cb.validate(fmt14);
        assertNotNull(cb.getProperties().get(PropertyMap.RESULT_TYPE_PROP));

        ColumnBuilder cb2 = new ColumnBuilder("c2", DataType.LONG).withCalculatedInfo("[a]+[b]");
        assertTrue(cb2.isCalculated());
        cb2.validate(fmt14);
    }

    @Test
    void testEscapeAndToString() {
        ColumnBuilder cb = new ColumnBuilder("value", DataType.TEXT);
        assertEquals("xvalue", cb.escapeName().getName());
        assertSame(cb, cb.toColumn());
        String str = cb.toString();
        assertTrue(str.startsWith("ColumnBuilder["));
        assertTrue(str.contains("type=TEXT"));
    }

}
