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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

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
    void twoTables(TestDb testDb) throws Exception {

        try (Database db = testDb.open()) {
            Table t1 = db.getTable("Table1");
            Table t2 = db.getTable("Table2");
            Table t3 = db.getTable("Table3");

            List<Relationship> rels = db.getRelationships(t1, t2);
            assertThat(rels.size()).isEqualTo(1);
            Relationship rel = rels.get(0);
            assertThat(rel.getName()).isEqualTo("Table2Table1");
            assertThat(rel.getFromTable()).isEqualTo(t2);
            assertThat(rel.getFromColumns()).isEqualTo(List.of(t2.getColumn("id")));
            assertThat(rel.getToTable()).isEqualTo(t1);
            assertThat(rel.getToColumns()).isEqualTo(List.of(t1.getColumn("otherfk1")));
            assertThat(rel.hasReferentialIntegrity()).isTrue();
            assertThat(((RelationshipImpl) rel).getFlags()).isEqualTo(4096);
            assertThat(rel.cascadeDeletes()).isTrue();
            assertSameRelationships(rels, db.getRelationships(t2, t1), true);

            rels = db.getRelationships(t2, t3);
            assertThat(db.getRelationships(t2, t3).isEmpty()).isTrue();
            assertSameRelationships(rels, db.getRelationships(t3, t2), true);

            rels = db.getRelationships(t1, t3);
            assertThat(rels.size()).isEqualTo(1);
            rel = rels.get(0);
            assertThat(rel.getName()).isEqualTo("Table3Table1");
            assertThat(rel.getFromTable()).isEqualTo(t3);
            assertThat(rel.getFromColumns()).isEqualTo(List.of(t3.getColumn("id")));
            assertThat(rel.getToTable()).isEqualTo(t1);
            assertThat(rel.getToColumns()).isEqualTo(List.of(t1.getColumn("otherfk2")));
            assertThat(rel.hasReferentialIntegrity()).isTrue();
            assertThat(((RelationshipImpl) rel).getFlags()).isEqualTo(256);
            assertThat(rel.cascadeUpdates()).isTrue();
            assertSameRelationships(rels, db.getRelationships(t3, t1), true);

            assertThrows(IllegalArgumentException.class, () -> db.getRelationships(t1, t1));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(INDEX)
    void oneTable(TestDb testDb) throws Exception {
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
    void noTables(TestDb testDb) throws Exception {
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
        assertThat(found.size()).isEqualTo(expected.size());
        if (!ordered) {
            expected.sort(REL_COMP);
            found.sort(REL_COMP);
        }
        for (int i = 0; i < expected.size(); i++) {
            Relationship eRel = expected.get(i);
            Relationship fRel = found.get(i);
            assertThat(fRel.getName()).isEqualTo(eRel.getName());
        }
    }

}
