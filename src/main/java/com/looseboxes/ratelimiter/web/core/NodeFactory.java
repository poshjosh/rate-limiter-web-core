package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.node.Node;

import java.util.function.BiConsumer;

public interface NodeFactory<S, V> {

    Node<NodeData<V>> createNode(String name, S source, BiConsumer<Object, Node<NodeData<V>>> nodeConsumer);
}
