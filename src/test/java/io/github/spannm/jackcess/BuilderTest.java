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
import io.github.spannm.jackcess.Relationship.JoinType;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.impl.RelationshipImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link TableBuilder}, {@link IndexBuilder}, {@link RelationshipBuilder} and {@link DatabaseBuilder}.
 */
class BuilderTest extends AbstractBaseTest {

    @Test
    void tableBuilderEscaping() {
        TableBuilder tb = new TableBuilder("Table", true);
        assertThat(tb.getName()).isEqualTo("xTable");
        tb.addColumn(new ColumnBuilder("value", DataType.TEXT));
        assertThat(tb.getColumns().get(0).getName()).isEqualTo("xvalue");

        tb.addIndex(new IndexBuilder("index").withColumns("value"));
        assertThat(tb.getIndexes().get(0).getName()).isEqualTo("xindex");
        assertThat(tb.getIndexes().get(0).getColumns().get(0).getName()).isEqualTo("xvalue");

        TableBuilder tb2 = new TableBuilder("Table").escapeName();
        assertThat(tb2.getName()).isEqualTo("xTable");
        assertThat(TableBuilder.isReservedWord("select")).isTrue();
        assertThat(TableBuilder.isReservedWord("notareservedword")).isFalse();
    }

    @Test
    void tableBuilderCollections() {
        TableBuilder tb = new TableBuilder("t")
            .withEscapeIdentifiers(false)
            .addColumns(null)
            .addColumns(List.of(new ColumnBuilder("a", DataType.LONG), new ColumnBuilder("b", DataType.TEXT)))
            .addIndexes(null)
            .addIndexes(List.of(new IndexBuilder("idx").withColumns("a")));
        assertThat(tb.getColumns().size()).isEqualTo(2);
        assertThat(tb.getIndexes().size()).isEqualTo(1);

        assertThat(tb.getProperties()).isNull();
        tb.putProperty("p1", "v1").putProperty("p2", DataType.LONG, 7);
        assertThat(tb.getProperties().get("p1").getValue()).isEqualTo("v1");
        assertThat(tb.getProperties().get("p2").getValue()).isEqualTo(7);

        String str = tb.toString();
        assertThat(str.startsWith("TableBuilder[")).isTrue();
        assertThat(str.contains("name=t")).isTrue();
    }

    @Test
    void indexBuilder() {
        IndexBuilder ib = new IndexBuilder("idx");
        assertThat(ib.isUnique()).isFalse();
        assertThat(ib.isIgnoreNulls()).isFalse();
        assertThat(ib.isPrimaryKey()).isFalse();

        ib.withName("idx2").withColumns("a", "b").withIgnoreNulls().withUnique();
        assertThat(ib.getName()).isEqualTo("idx2");
        assertThat(ib.isUnique()).isTrue();
        assertThat(ib.isIgnoreNulls()).isTrue();
        assertThat(ib.getColumns().size()).isEqualTo(2);
        assertThat(ib.getColumns().get(0).isAscending()).isTrue();

        IndexBuilder.Column col = ib.getColumns().get(0);
        assertThat(col.getName()).isEqualTo("a");
        col.withName("a2");
        assertThat(col.getName()).isEqualTo("a2");

        IndexBuilder desc = new IndexBuilder("d").withColumns(false, "x");
        assertThat(desc.getColumns().get(0).isAscending()).isFalse();

        assertThat(new IndexBuilder("n").withColumns((String[]) null).getColumns().size()).isEqualTo(0);
        assertThat(new IndexBuilder("n").withColumns(List.of("q")).getColumns().size()).isEqualTo(1);

        IndexBuilder pk = new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).withColumns("a").withPrimaryKey();
        assertThat(pk.isPrimaryKey()).isTrue();
        pk.setIndexNumber(4);
        assertThat(pk.getIndexNumber()).isEqualTo(4);
    }

    @Test
    void indexBuilderValidate() {
        JetFormat fmt = JetFormat.VERSION_4;
        Set<String> colNames = Set.of("A", "B");

        assertThrows(IllegalArgumentException.class,
            () -> new IndexBuilder("idx").validate(colNames, fmt));

        IndexBuilder tooMany = new IndexBuilder("idx");
        for (int i = 0; i < 20; i++) {
            tooMany.withColumns("c" + i);
        }
        assertThrows(IllegalArgumentException.class, () -> tooMany.validate(colNames, fmt));

        assertThrows(IllegalArgumentException.class,
            () -> new IndexBuilder("idx").withColumns("a", "A").validate(colNames, fmt));
        assertThrows(IllegalArgumentException.class,
            () -> new IndexBuilder("idx").withColumns("zz").validate(colNames, fmt));

        new IndexBuilder("idx").withColumns("a", "b").validate(colNames, fmt);
    }

    @Test
    void relationshipBuilder() throws Exception {
        RelationshipBuilder rb = new RelationshipBuilder("from", "to")
            .withName("rel")
            .withCascadeDeletes()
            .withCascadeUpdates()
            .withCascadeNullOnDelete()
            .withReferentialIntegrity();
        assertThat(rb.hasReferentialIntegrity()).isTrue();
        assertThat(rb.getName()).isEqualTo("rel");
        assertThat(rb.getFromTable()).isEqualTo("from");
        assertThat(rb.getToTable()).isEqualTo("to");
        assertThat(rb.getFlags() & RelationshipImpl.CASCADE_UPDATES_FLAG).isNotEqualTo(0);
        assertThat(rb.getFlags() & RelationshipImpl.CASCADE_NULL_FLAG).isNotEqualTo(0);

        rb.withJoinType(JoinType.LEFT_OUTER);
        assertThat(rb.getFlags() & RelationshipImpl.LEFT_OUTER_JOIN_FLAG).isNotEqualTo(0);
        rb.withJoinType(JoinType.RIGHT_OUTER);
        assertThat(rb.getFlags() & RelationshipImpl.RIGHT_OUTER_JOIN_FLAG).isNotEqualTo(0);
        assertThat(rb.getFlags() & RelationshipImpl.LEFT_OUTER_JOIN_FLAG).isEqualTo(0);
        rb.withJoinType(JoinType.INNER);
        assertThat(rb.getFlags() & RelationshipImpl.RIGHT_OUTER_JOIN_FLAG).isEqualTo(0);

        try (Database db = createDbMem(FileFormat.V2000)) {
            TestUtil.createTestTable(db);
            Table table = db.getTable("test");
            Column col = table.getColumns().iterator().next();
            RelationshipBuilder rb2 = new RelationshipBuilder(table, table).addColumns(col, col);
            assertThat(rb2.getFromTable()).isEqualTo(table.getName());
            assertThat(rb2.getFromColumns()).isEqualTo(List.of(col.getName()));
            assertThat(rb2.getToColumns()).isEqualTo(List.of(col.getName()));
        }
    }

    @Test
    void databaseBuilderConvenience() {
        assertThat(DatabaseBuilder.newDatabase()).isNotNull();
        assertThat(DatabaseBuilder.newDatabase(new File("x.mdb").toPath())).isNotNull();
        assertThat(DatabaseBuilder.newDatabase(new File("x.mdb"))).isNotNull();
        assertThat(DatabaseBuilder.newTable("t").getName()).isEqualTo("t");
        assertThat(DatabaseBuilder.newTable("value", true).getName()).isEqualTo("xvalue");
        assertThat(DatabaseBuilder.newColumn("c").getName()).isEqualTo("c");
        assertThat(DatabaseBuilder.newColumn("c", DataType.LONG).getType()).isEqualTo(DataType.LONG);
        assertThat(DatabaseBuilder.newIndex("i").getName()).isEqualTo("i");
        assertThat(DatabaseBuilder.newPrimaryKey("a").isPrimaryKey()).isTrue();
        assertThat(DatabaseBuilder.newRelationship("a", "b")).isNotNull();
    }

    @Test
    void databaseBuilderProperties() throws Exception {
        File file = TestUtil.createTempFile(getShortTestMethodName(), Database.FILE_EXT_MDB, false);
        try (Database db = new DatabaseBuilder()
            .withFile(file)
            .withFileFormat(FileFormat.V2000)
            .withTimeZone(TEST_TZ)
            .withAutoSync(getTestAutoSync())
            .putDatabaseProperty("dbProp", "dbVal")
            .putSummaryProperty(PropertyMap.TITLE_PROP, "myTitle")
            .putUserDefinedProperty("userProp", DataType.TEXT, "userVal")
            .create()) {

            assertThat(db.getDatabaseProperties().getValue("dbProp")).isEqualTo("dbVal");
            assertThat(db.getSummaryProperties().getValue(PropertyMap.TITLE_PROP)).isEqualTo("myTitle");
            assertThat(db.getUserDefinedProperties().getValue("userProp")).isEqualTo("userVal");
        }

        try (Database db = DatabaseBuilder.open(file.toPath())) {
            assertThat(db.getDatabaseProperties().getValue("dbProp")).isEqualTo("dbVal");
        }
        try (Database db = DatabaseBuilder.open(file)) {
            assertThat(db.getFile()).isNotNull();
        }
    }

    @Test
    void databaseBuilderCreateStatic() throws Exception {
        File file = TestUtil.createTempFile(getShortTestMethodName(), Database.FILE_EXT_MDB, false);
        try (Database db = DatabaseBuilder.create(FileFormat.V2000, file)) {
            assertThat(Arrays.asList(FileFormat.values()).contains(db.getFileFormat())).isTrue();
        }
    }

}
