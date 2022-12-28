package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;

public interface NodeBuilder<S, V> {

    Node<NodeValue<V>> buildNode(String name, S source, AnnotationProcessor.NodeConsumer<V> nodeConsumer);
}
