package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.WebRequestRateLimiterConfig;

import java.util.*;
import java.util.function.BiConsumer;

public class WebRequestRateLimiter<R> implements RateLimiter<R>{

    private static class CollectNodeNames implements
            BiConsumer<Object, Node<NodeData<RateConfigList>>> {
        private Set<String> nodeNames;
        @Override public void accept(Object o, Node<NodeData<RateConfigList>> node) {
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
            BiConsumer<Object, Node<NodeData<RateConfigList>>> {
        private final Set<String> alreadyUsedNodeName;
        public RequireUniqueName(Set<String> alreadyUsedNodeName) {
            this.alreadyUsedNodeName = Objects.requireNonNull(alreadyUsedNodeName);
        }
        @Override public void accept(Object source, Node<NodeData<RateConfigList>> node) {
            if(node != null && alreadyUsedNodeName.contains(node.getName())) {
                throw new IllegalStateException("Already used. Node name: " + node.getName());
            }
        }
    }

    private final RateLimiter<R> rateLimiter;

    public WebRequestRateLimiter(WebRequestRateLimiterConfig<R> webRequestRateLimiterConfig) {

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

    @Override public boolean increment(R resourceId) {
        return rateLimiter.increment(resourceId);
    }

    @Override public boolean increment(R resourceId, int amount) {
        return rateLimiter.increment(resourceId, amount);
    }

    @Override public boolean increment(Object resource, R resourceId, int amount) {
        return rateLimiter.increment(resource, resourceId, amount);
    }
}