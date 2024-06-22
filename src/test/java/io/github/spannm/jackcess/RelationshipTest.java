/*
Copyright (c) 2008 Health Market Science, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.INDEX;

import io.github.spannm.jackcess.impl.RelationshipImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class RelationshipTest extends AbstractBaseTest {

    private static final Comparator<Relationship> REL_COMP = (r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName());

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(INDEX)
    void testTwoTables(TestDb testDb) throws Exception {

        try (Database db = testDb.open()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            List<Relationship> rels = db.getRelationships(t1, t2);
            assertEquals(1, rels.size());
            Relationship rel = rels.get(0);
            assertEquals("Table2Table1", rel.getName());
            assertEquals(t2, rel.getFromTable());
            assertEquals(List.of(t2.getColumn("id")),
                rel.getFromColumns());
            assertEquals(t1, rel.getToTable());
            assertEquals(List.of(t1.getColumn("otherfk1")),
                rel.getToColumns());
            assertTrue(rel.hasReferentialIntegrity());
            assertEquals(4096, ((RelationshipImpl) rel).getFlags());
            assertTrue(rel.cascadeDeletes());
            assertSameRelationships(rels, db.getRelationships(t2, t1), true);

            rels = db.getRelationships(t2, t3);
            assertTrue(db.getRelationships(t2, t3).isEmpty());
            assertSameRelationships(rels, db.getRelationships(t3, t2), true);

            rels = db.getRelationships(t1, t3);
            assertEquals(1, rels.size());
            rel = rels.get(0);
            assertEquals("Table3Table1", rel.getName());
            assertEquals(t3, rel.getFromTable());
            assertEquals(List.of(t3.getColumn("id")),
                rel.getFromColumns());
            assertEquals(t1, rel.getToTable());
            assertEquals(List.of(t1.getColumn("otherfk2")),
                rel.getToColumns());
            assertTrue(rel.hasReferentialIntegrity());
            assertEquals(256, ((RelationshipImpl) rel).getFlags());
            assertTrue(rel.cascadeUpdates());
            assertSameRelationships(rels, db.getRelationships(t3, t1), true);

            assertThrows(IllegalArgumentException.class, () -> db.getRelationships(t1, t1));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(INDEX)
    void testOneTable(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            List<Relationship> expected = new ArrayList<>();
            expected.addAll(db.getRelationships(t1, t2));
            expected.addAll(db.getRelationships(t2, t3));

            assertSameRelationships(expected, db.getRelationships(t2), false);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(INDEX)
    void testNoTables(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            List<Relationship> expected = new ArrayList<>();
            expected.addAll(db.getRelationships(t1, t2));
            expected.addAll(db.getRelationships(t2, t3));
            expected.addAll(db.getRelationships(t1, t3));

            assertSameRelationships(expected, db.getRelationships(), false);
        }
    }

    private static void assertSameRelationships(
        List<Relationship> expected, List<Relationship> found, boolean ordered) {
        assertEquals(expected.size(), found.size());
        if (!ordered) {
            expected.sort(REL_COMP);
            found.sort(REL_COMP);
        }
        for (int i = 0; i < expected.size(); i++) {
            Relationship eRel = expected.get(i);
            Relationship fRel = found.get(i);
            assertEquals(eRel.getName(), fRel.getName());
        }
    }

}
