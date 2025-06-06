package io.github.spannm.jackcess.impl;

import java.util.ArrayList;
import java.util.List;

public abstract class TopoSorter<E> {
    public static final boolean REVERSE   = true;

    // https://en.wikipedia.org/wiki/Topological_sorting
    private static final int    UNMARKED  = 0;
    private static final int    TEMP_MARK = 1;
    private static final int    PERM_MARK = 2;

    private final List<E>       _values;
    private final List<Node<E>> _nodes    = new ArrayList<>();
    private final boolean       _reverse;

    protected TopoSorter(List<E> values, boolean reverse) {
        _values = values;
        _reverse = reverse;
    }

    public void sort() {

        for (E val : _values) {
            Node<E> node = new Node<>(val);
            fillDescendents(val, node._descs);

            // build the internal list in reverse so that we maintain the "original"
            // order of items which we don't need to re-arrange
            _nodes.add(0, node);
        }

        _values.clear();

        for (Node<E> node : _nodes) {
            if (node._mark != UNMARKED) {
                continue;
            }

            visit(node);
        }
    }

    private void visit(Node<E> node) {

        if (node._mark == PERM_MARK) {
            return;
        }

        if (node._mark == TEMP_MARK) {
            throw new IllegalStateException("Cycle detected");
        }

        node._mark = TEMP_MARK;

        for (E descVal : node._descs) {
            Node<E> desc = findDescendent(descVal);
            visit(desc);
        }

        node._mark = PERM_MARK;

        if (_reverse) {
            _values.add(node._val);
        } else {
            _values.add(0, node._val);
        }
    }

    private Node<E> findDescendent(E val) {
        for (Node<E> node : _nodes) {
            if (node._val == val) {
                return node;
            }
        }
        throw new IllegalStateException("Unknown descendent " + val);
    }

    protected abstract void fillDescendents(E from, List<E> descendents);

    private static class Node<E> {
        private final E       _val;
        private final List<E> _descs = new ArrayList<>();
        private int           _mark  = UNMARKED;

        private Node(E val) {
            _val = val;
        }
    }
}
