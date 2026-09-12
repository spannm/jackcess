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
import io.github.spannm.jackcess.Relationship.JoinType;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.impl.RelationshipImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link TableBuilder}, {@link IndexBuilder}, {@link RelationshipBuilder} and {@link DatabaseBuilder}.
 */
class BuilderTest extends AbstractBaseTest {

    @Test
    void testTableBuilderEscaping() {
        TableBuilder tb = new TableBuilder("Table", true);
        assertEquals("xTable", tb.getName());
        tb.addColumn(new ColumnBuilder("value", DataType.TEXT));
        assertEquals("xvalue", tb.getColumns().get(0).getName());

        tb.addIndex(new IndexBuilder("index").withColumns("value"));
        assertEquals("xindex", tb.getIndexes().get(0).getName());
        assertEquals("xvalue", tb.getIndexes().get(0).getColumns().get(0).getName());

        TableBuilder tb2 = new TableBuilder("Table").escapeName();
        assertEquals("xTable", tb2.getName());
        assertTrue(TableBuilder.isReservedWord("select"));
        assertFalse(TableBuilder.isReservedWord("notareservedword"));
    }

    @Test
    void testTableBuilderCollections() {
        TableBuilder tb = new TableBuilder("t")
            .withEscapeIdentifiers(false)
            .addColumns(null)
            .addColumns(List.of(new ColumnBuilder("a", DataType.LONG), new ColumnBuilder("b", DataType.TEXT)))
            .addIndexes(null)
            .addIndexes(List.of(new IndexBuilder("idx").withColumns("a")));
        assertEquals(2, tb.getColumns().size());
        assertEquals(1, tb.getIndexes().size());

        assertNull(tb.getProperties());
        tb.putProperty("p1", "v1").putProperty("p2", DataType.LONG, 7);
        assertEquals("v1", tb.getProperties().get("p1").getValue());
        assertEquals(7, tb.getProperties().get("p2").getValue());

        String str = tb.toString();
        assertTrue(str.startsWith("TableBuilder["));
        assertTrue(str.contains("name=t"));
    }

    @Test
    void testIndexBuilder() {
        IndexBuilder ib = new IndexBuilder("idx");
        assertFalse(ib.isUnique());
        assertFalse(ib.isIgnoreNulls());
        assertFalse(ib.isPrimaryKey());

        ib.withName("idx2").withColumns("a", "b").withIgnoreNulls().withUnique();
        assertEquals("idx2", ib.getName());
        assertTrue(ib.isUnique());
        assertTrue(ib.isIgnoreNulls());
        assertEquals(2, ib.getColumns().size());
        assertTrue(ib.getColumns().get(0).isAscending());

        IndexBuilder.Column col = ib.getColumns().get(0);
        assertEquals("a", col.getName());
        col.withName("a2");
        assertEquals("a2", col.getName());

        IndexBuilder desc = new IndexBuilder("d").withColumns(false, "x");
        assertFalse(desc.getColumns().get(0).isAscending());

        assertEquals(0, new IndexBuilder("n").withColumns((String[]) null).getColumns().size());
        assertEquals(1, new IndexBuilder("n").withColumns(List.of("q")).getColumns().size());

        IndexBuilder pk = new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).withColumns("a").withPrimaryKey();
        assertTrue(pk.isPrimaryKey());
        pk.setIndexNumber(4);
        assertEquals(4, pk.getIndexNumber());
    }

    @Test
    void testIndexBuilderValidate() {
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
    void testRelationshipBuilder() throws IOException {
        RelationshipBuilder rb = new RelationshipBuilder("from", "to")
            .withName("rel")
            .withCascadeDeletes()
            .withCascadeUpdates()
            .withCascadeNullOnDelete()
            .withReferentialIntegrity();
        assertTrue(rb.hasReferentialIntegrity());
        assertEquals("rel", rb.getName());
        assertEquals("from", rb.getFromTable());
        assertEquals("to", rb.getToTable());
        assertNotEquals(0, rb.getFlags() & RelationshipImpl.CASCADE_UPDATES_FLAG);
        assertNotEquals(0, rb.getFlags() & RelationshipImpl.CASCADE_NULL_FLAG);

        rb.withJoinType(JoinType.LEFT_OUTER);
        assertNotEquals(0, rb.getFlags() & RelationshipImpl.LEFT_OUTER_JOIN_FLAG);
        rb.withJoinType(JoinType.RIGHT_OUTER);
        assertNotEquals(0, rb.getFlags() & RelationshipImpl.RIGHT_OUTER_JOIN_FLAG);
        assertEquals(0, rb.getFlags() & RelationshipImpl.LEFT_OUTER_JOIN_FLAG);
        rb.withJoinType(JoinType.INNER);
        assertEquals(0, rb.getFlags() & RelationshipImpl.RIGHT_OUTER_JOIN_FLAG);

        try (Database db = createDbMem(FileFormat.V2000)) {
            TestUtil.createTestTable(db);
            Table table = db.getTable("test");
            Column col = table.getColumns().iterator().next();
            RelationshipBuilder rb2 = new RelationshipBuilder(table, table).addColumns(col, col);
            assertEquals(table.getName(), rb2.getFromTable());
            assertEquals(List.of(col.getName()), rb2.getFromColumns());
            assertEquals(List.of(col.getName()), rb2.getToColumns());
        }
    }

    @Test
    void testDatabaseBuilderConvenience() {
        assertNotNull(DatabaseBuilder.newDatabase());
        assertNotNull(DatabaseBuilder.newDatabase(new File("x.mdb").toPath()));
        assertNotNull(DatabaseBuilder.newDatabase(new File("x.mdb")));
        assertEquals("t", DatabaseBuilder.newTable("t").getName());
        assertEquals("xvalue", DatabaseBuilder.newTable("value", true).getName());
        assertEquals("c", DatabaseBuilder.newColumn("c").getName());
        assertEquals(DataType.LONG, DatabaseBuilder.newColumn("c", DataType.LONG).getType());
        assertEquals("i", DatabaseBuilder.newIndex("i").getName());
        assertTrue(DatabaseBuilder.newPrimaryKey("a").isPrimaryKey());
        assertNotNull(DatabaseBuilder.newRelationship("a", "b"));
    }

    @Test
    void testDatabaseBuilderProperties() throws IOException {
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

            assertEquals("dbVal", db.getDatabaseProperties().getValue("dbProp"));
            assertEquals("myTitle", db.getSummaryProperties().getValue(PropertyMap.TITLE_PROP));
            assertEquals("userVal", db.getUserDefinedProperties().getValue("userProp"));
        }

        try (Database db = DatabaseBuilder.open(file.toPath())) {
            assertEquals("dbVal", db.getDatabaseProperties().getValue("dbProp"));
        }
        try (Database db = DatabaseBuilder.open(file)) {
            assertNotNull(db.getFile());
        }
    }

    @Test
    void testDatabaseBuilderCreateStatic() throws IOException {
        File file = TestUtil.createTempFile(getShortTestMethodName(), Database.FILE_EXT_MDB, false);
        try (Database db = DatabaseBuilder.create(FileFormat.V2000, file)) {
            assertTrue(Arrays.asList(FileFormat.values()).contains(db.getFileFormat()));
        }
    }

}
