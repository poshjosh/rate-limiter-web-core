package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.node.Node;

import java.util.function.BiConsumer;

public interface NodeBuilder<S, V> {

    Node<NodeData<V>> buildNode(String name, S source, BiConsumer<Object, Node<NodeData<V>>> nodeConsumer);
}
