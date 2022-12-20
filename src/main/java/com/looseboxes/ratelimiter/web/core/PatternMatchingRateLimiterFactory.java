package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.CompositeRate;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.node.Node;

import java.util.function.BiConsumer;

public interface PatternMatchingRateLimiterFactory<R> {

    default RateLimiter<R> createRateLimiter(String rootNodeName) {
        return createRateLimiter(rootNodeName, (source, node) -> { });
    }

    RateLimiter<R> createRateLimiter(String rootNodeName,
            BiConsumer<Object, Node<NodeData<CompositeRate>>> nodeConsumer);
}
