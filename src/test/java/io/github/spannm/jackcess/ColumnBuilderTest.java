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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ColumnBuilder}.
 */
class ColumnBuilderTest extends AbstractBaseTest {

    @Test
    void maxPrecisionAndScale() {
        ColumnBuilder cb = new ColumnBuilder("num", DataType.NUMERIC)
            .withMaxPrecision().withMaxScale();
        assertThat(cb.getPrecision()).isEqualTo((byte) DataType.NUMERIC.getMaxPrecision());
        assertThat(cb.getScale()).isEqualTo((byte) DataType.NUMERIC.getMaxScale());

        // types without scale/precision are left untouched
        ColumnBuilder txt = new ColumnBuilder("txt", DataType.TEXT)
            .withMaxPrecision().withMaxScale();
        assertThat(txt.getPrecision()).isEqualTo((byte) DataType.TEXT.getDefaultPrecision());
        assertThat(txt.getScale()).isEqualTo((byte) DataType.TEXT.getDefaultScale());
    }

    @Test
    void maxLength() {
        ColumnBuilder txt = new ColumnBuilder("txt", DataType.TEXT).withMaxLength();
        assertThat(txt.getLength()).isEqualTo((short) DataType.TEXT.getMaxSize());

        // fixed length types are left untouched
        ColumnBuilder lng = new ColumnBuilder("lng", DataType.LONG).withMaxLength();
        assertThat(lng.getLength()).isEqualTo((short) DataType.LONG.getFixedSize());
        assertThat(lng.getFixedDataSize()).isEqualTo(DataType.LONG.getFixedSize());
    }

    @Test
    void flags() {
        ColumnBuilder cb = new ColumnBuilder("memo", DataType.MEMO)
            .withCompressedUnicode(true)
            .withHyperlink(true)
            .withAutoNumber(false);
        assertThat(cb.isCompressedUnicode()).isTrue();
        assertThat(cb.isHyperlink()).isTrue();
        assertThat(cb.isAutoNumber()).isFalse();
        assertThat(cb.isVariableLength()).isTrue();
        assertThat(cb.storeInNullMask()).isFalse();
        assertThat(new ColumnBuilder("b", DataType.BOOLEAN).storeInNullMask()).isTrue();

        cb.setColumnNumber((short) 3);
        assertThat(cb.getColumnNumber()).isEqualTo((short) 3);
        cb.setTextSortOrder(ColumnImpl.GENERAL_SORT_ORDER);
        assertThat(cb.getTextSortOrder()).isEqualTo(ColumnImpl.GENERAL_SORT_ORDER);
    }

    @Test
    void properties() {
        ColumnBuilder cb = new ColumnBuilder("c", DataType.TEXT);
        assertThat(cb.getProperties()).isNull();
        cb.withProperty("myprop", "myval");
        assertThat(cb.getProperties()).isNotNull();
        assertThat(cb.getProperties().get("myprop").getValue()).isEqualTo("myval");
        cb.withProperty("intprop", DataType.LONG, 13);
        assertThat(cb.getProperties().get("intprop").getValue()).isEqualTo(13);
    }

    @Test
    void fromColumnBuilder() {
        ColumnBuilder template = new ColumnBuilder("tmpl", DataType.NUMERIC)
            .withScale(3).withPrecision(10)
            .withAutoNumber(false)
            .withCompressedUnicode(true)
            .withHyperlink(true)
            .withCalculated(false)
            .withProperty("aprop", "aval");
        template.setTextSortOrder(ColumnImpl.GENERAL_SORT_ORDER);

        ColumnBuilder copy = new ColumnBuilder("copy").withFromColumn(template);
        assertThat(copy.getType()).isEqualTo(DataType.NUMERIC);
        assertThat(copy.getScale()).isEqualTo((byte) 3);
        assertThat(copy.getPrecision()).isEqualTo((byte) 10);
        assertThat(copy.isCompressedUnicode()).isTrue();
        assertThat(copy.isHyperlink()).isTrue();
        assertThat(copy.isCalculated()).isFalse();
        assertThat(copy.getTextSortOrder()).isEqualTo(ColumnImpl.GENERAL_SORT_ORDER);
        assertThat(copy.getProperties().get("aprop").getValue()).isEqualTo("aval");
        assertThat(copy.getName()).isEqualTo("copy");
    }

    @Test
    void fromColumn() throws Exception {
        try (Database db = createDbMem(FileFormat.V2000)) {
            TestUtil.createTestTable(db);
            Table table = db.getTable("test");
            Column src = table.getColumns().iterator().next();

            ColumnBuilder cb = new ColumnBuilder("copy").withFromColumn(src);
            assertThat(cb.getType()).isEqualTo(src.getType());
            assertThat(cb.isAutoNumber()).isEqualTo(src.isAutoNumber());
            assertThat(cb.isCalculated()).isEqualTo(src.isCalculated());
            assertThat(cb.isHyperlink()).isEqualTo(src.isHyperlink());
        }
    }

    @Test
    void sqlType() throws Exception {
        ColumnBuilder cb = new ColumnBuilder("c").withSqlType(java.sql.Types.INTEGER);
        assertThat(cb.getType()).isEqualTo(DataType.LONG);
        cb = new ColumnBuilder("c").withSqlType(java.sql.Types.VARCHAR, 100);
        assertThat(cb.getType()).isEqualTo(DataType.TEXT);
        cb = new ColumnBuilder("c").withSqlType(java.sql.Types.VARCHAR, 100, FileFormat.V2003);
        assertThat(cb.getType()).isEqualTo(DataType.TEXT);
    }

    @Test
    void validate() {
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
    void validateCalculated() {
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
        assertThat(cb.getProperties().get(PropertyMap.RESULT_TYPE_PROP)).isNotNull();

        ColumnBuilder cb2 = new ColumnBuilder("c2", DataType.LONG).withCalculatedInfo("[a]+[b]");
        assertThat(cb2.isCalculated()).isTrue();
        cb2.validate(fmt14);
    }

    @Test
    void escapeAndToString() {
        ColumnBuilder cb = new ColumnBuilder("value", DataType.TEXT);
        assertThat(cb.escapeName().getName()).isEqualTo("xvalue");
        assertThat(cb.toColumn()).isSameAs(cb);
        String str = cb.toString();
        assertThat(str.startsWith("ColumnBuilder[")).isTrue();
        assertThat(str.contains("type=TEXT")).isTrue();
    }

}
