package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.node.Node;

class Checks {
    private static final class NodeValueAbsentException extends RuntimeException{
        public NodeValueAbsentException() {
            super("Value is required for all non-root nodes");
        }
    }
    static <V> V requireNodeValue(Node<V> node) {
        return node.getValueOptional().orElseThrow(NodeValueAbsentException::new);
    }
}
