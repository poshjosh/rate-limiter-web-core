package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.exceptions.NodeValueAbsentException;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.Arrays;
import java.util.Objects;

final class Checks {
    private Checks() { }
    static RuntimeException notSupported(Object complainer, Object unsupported) {
        return notSupported(complainer.getClass(), unsupported);
    }
    static RuntimeException notSupported(Class<?> complainer, Object unsupported) {
        return new UnsupportedOperationException(
                complainer.getSimpleName() + " does not support: " + unsupported
        );
    }
    static <V> V requireNodeValue(Node<V> node) {
        return node.getValueOptional().orElseThrow(() -> new NodeValueAbsentException(node));
    }
    static void requireParentNameDoesNotMatchChild(String parent, String child) {
        if ( Objects.equals(parent, child)) {
            throw new IllegalStateException(
                    "Parent and child nodes may not have the same name: " + parent);
        }
    }
    static RuntimeException illegal(Enum en) {
        return new IllegalArgumentException("Unexpected " +
                en.getDeclaringClass().getSimpleName() + ": " + en +
                ", supported: " + Arrays.toString(en.getClass().getEnumConstants()));

    }
}
