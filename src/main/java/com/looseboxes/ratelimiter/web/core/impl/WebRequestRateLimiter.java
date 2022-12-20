package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.util.CompositeRate;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.web.core.PatternMatchingRateLimiterConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class WebRequestRateLimiter<R> implements RateLimiter<R>{

    private static class CollectNodeNames implements
            BiConsumer<Object, Node<NodeData<CompositeRate>>> {
        private Set<String> nodeNames;
        @Override public void accept(Object o, Node<NodeData<CompositeRate>> node) {
            if (nodeNames == null) {
                nodeNames = new HashSet<>();
            }
            nodeNames.add(node.getName());
        }
        public Set<String> getNodeNames() {
            return nodeNames == null ? Collections.emptySet() : Collections.unmodifiableSet(nodeNames);
        }
    }

    private static class RequireUniqueName implements
            BiConsumer<Object, Node<NodeData<CompositeRate>>> {
        private final Set<String> alreadyUsedNodeName;
        public RequireUniqueName(Set<String> alreadyUsedNodeName) {
            this.alreadyUsedNodeName = Objects.requireNonNull(alreadyUsedNodeName);
        }
        @Override public void accept(Object source, Node<NodeData<CompositeRate>> node) {
            if(node != null && alreadyUsedNodeName.contains(node.getName())) {
                throw new IllegalStateException("Already used. Node name: " + node.getName());
            }
        }
    }

    private final RateLimiter<R> rateLimiter;

    public WebRequestRateLimiter(PatternMatchingRateLimiterConfig<R> webRequestRateLimiterConfig) {

        CollectNodeNames collectNodeNames = new CollectNodeNames();
        RateLimiter<R> rateLimiterForProperties = webRequestRateLimiterConfig
                .getPatternMatchingRateLimiterFactoryForProperties()
                .createRateLimiter("root.properties", collectNodeNames);

        RequireUniqueName requireUniqueName = new RequireUniqueName(collectNodeNames.getNodeNames());
        RateLimiter<R> rateLimiterForAnnotations = webRequestRateLimiterConfig
                .getPatternMatchingRateLimiterFactoryForAnnotations()
                .createRateLimiter("root.annotations", requireUniqueName);

        this.rateLimiter = rateLimiterForProperties.andThen(rateLimiterForAnnotations);
    }

    @Override
    public boolean tryConsume(Object context, R resourceId, int permits, long timeout, TimeUnit unit) {
        return rateLimiter.tryConsume(context, resourceId, permits, timeout, unit);
    }
}
