/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
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
package io.github.spannm.jackcess.impl;

import java.util.ArrayList;
import java.util.List;

public abstract class TopoSorter<E> {
    public static final boolean REVERSE   = true;

    // https://en.wikipedia.org/wiki/Topological_sorting
    private static final int    UNMARKED  = 0;
    private static final int    TEMP_MARK = 1;
    private static final int    PERM_MARK = 2;

    private final List<E>       values;
    private final List<Node<E>> nodes    = new ArrayList<>();
    private final boolean       reverse;

    protected TopoSorter(List<E> values, boolean reverse) {
        this.values = values;
        this.reverse = reverse;
    }

    public void sort() {

        for (E val : values) {
            Node<E> node = new Node<>(val);
            fillDescendents(val, node.descs);

            // build the internal list in reverse so that we maintain the "original"
            // order of items which we don't need to re-arrange
            nodes.add(0, node);
        }

        values.clear();

        for (Node<E> node : nodes) {
            if (node.mark != UNMARKED) {
                continue;
            }

            visit(node);
        }
    }

    private void visit(Node<E> node) {

        if (node.mark == PERM_MARK) {
            return;
        }

        if (node.mark == TEMP_MARK) {
            throw new IllegalStateException("Cycle detected");
        }

        node.mark = TEMP_MARK;

        for (E descVal : node.descs) {
            Node<E> desc = findDescendent(descVal);
            visit(desc);
        }

        node.mark = PERM_MARK;

        if (reverse) {
            values.add(node.val);
        } else {
            values.add(0, node.val);
        }
    }

    private Node<E> findDescendent(E val) {
        for (Node<E> node : nodes) {
            if (node.val == val) {
                return node;
            }
        }
        throw new IllegalStateException("Unknown descendent " + val);
    }

    protected abstract void fillDescendents(E from, List<E> descendents);

    private static class Node<E> {
        private final E       val;
        private final List<E> descs = new ArrayList<>();
        private int           mark  = UNMARKED;

        private Node(E val) {
            this.val = val;
        }
    }
}
