/*
Copyright (c) 2018 James Ahlborn

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

package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 *
 * @author James Ahlborn
 */
class TopoSorterTest extends AbstractBaseTest {

    @Test
    void testTopoSort() {
        doTopoTest(List.of("A", "B", "C"),
            List.of("A", "B", "C"));

        doTopoTest(List.of("B", "A", "C"),
            List.of("A", "B", "C"),
            "B", "C",
            "A", "B");

        try {
            doTopoTest(List.of("B", "A", "C"),
                List.of("C", "B", "A"),
                "B", "C",
                "A", "B",
                "C", "A");
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException expected) {
            // success
            assertTrue(expected.getMessage().startsWith("Cycle"));
        }

        try {
            doTopoTest(List.of("B", "A", "C"),
                List.of("C", "B", "A"),
                "B", "D");
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException expected) {
            // success
            assertTrue(expected.getMessage().startsWith("Unknown descendent"));
        }

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("D", "A", "B", "C"),
            "B", "C",
            "A", "B");

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("A", "D", "B", "C"),
            "B", "C",
            "A", "B",
            "A", "D");

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("D", "A", "C", "B"),
            "D", "A",
            "C", "B");

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("D", "C", "A", "B"),
            "D", "A",
            "C", "B",
            "C", "A");

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("C", "D", "A", "B"),
            "D", "A",
            "C", "B",
            "C", "D");

        doTopoTest(List.of("B", "D", "A", "C"),
            List.of("D", "A", "C", "B"),
            "D", "A",
            "C", "B",
            "D", "B");
    }

    private static void doTopoTest(List<String> original,
        List<String> expected,
        String... descs) {

        List<String> values = new ArrayList<>(original);

        TestTopoSorter tsorter = new TestTopoSorter(values, false);
        for (int i = 0; i < descs.length; i += 2) {
            tsorter.addDescendents(descs[i], descs[i + 1]);
        }

        tsorter.sort();

        assertEquals(expected, values);

        values = new ArrayList<>(original);

        tsorter = new TestTopoSorter(values, true);
        for (int i = 0; i < descs.length; i += 2) {
            tsorter.addDescendents(descs[i], descs[i + 1]);
        }

        tsorter.sort();

        List<String> expectedReverse = new ArrayList<>(expected);
        Collections.reverse(expectedReverse);

        assertEquals(expectedReverse, values);
    }

    private static class TestTopoSorter extends TopoSorter<String> {
        private final Map<String, List<String>> _descMap = new HashMap<>();

        TestTopoSorter(List<String> values, boolean reverse) {
            super(values, reverse);
        }

        void addDescendents(String from, String... tos) {
            List<String> descs = _descMap.computeIfAbsent(from, k -> new ArrayList<>());

            descs.addAll(List.of(tos));
        }

        @Override
        protected void fillDescendents(String from, List<String> descendents) {
            List<String> descs = _descMap.get(from);
            if (descs != null) {
                descendents.addAll(descs);
            }
        }
    }
}
