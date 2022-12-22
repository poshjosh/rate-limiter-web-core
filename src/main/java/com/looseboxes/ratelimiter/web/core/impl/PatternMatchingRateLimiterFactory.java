package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import com.looseboxes.ratelimiter.web.core.Registries;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

final class PatternMatchingRateLimiterFactory<R, K, S>{

    private static final Logger LOG = LoggerFactory.getLogger(PatternMatchingRateLimiterFactory.class);

    private final S sourceOfRateLimitRules;
    private final NodeBuilder<S, Rates> nodeBuilder;
    private final Registries<R> registries;

    PatternMatchingRateLimiterFactory(
            S sourceOfRateLimitRules,
            NodeBuilder<S, Rates> nodeBuilder,
            Registries<R> registries) {
        this.sourceOfRateLimitRules = Objects.requireNonNull(sourceOfRateLimitRules);
        this.nodeBuilder = Objects.requireNonNull(nodeBuilder);
        this.registries = Objects.requireNonNull(registries);
    }

    public RateLimiter<R> createRateLimiter(String rootNodeName,
            BiConsumer<Object, Node<NodeData<Rates>>> nodeConsumer) {

        PatternMatchingRateLimiter.MatcherProvider<R> matcherProvider =
                (name, nodeData) -> registries.matchers().getOrCreateMatcher(name, nodeData.getSource());

        Node<NodeData<Rates>> rootNode =
                nodeBuilder.buildNode(rootNodeName, sourceOfRateLimitRules, nodeConsumer);

        Node<NodeData<RateLimiter<K>>> rootRateLimiterNode = toRateLimiterNode(rootNode);

        boolean matchAllNodesInTree = sourceOfRateLimitRules instanceof RateLimitProperties;

        return new PatternMatchingRateLimiter<>(matcherProvider, (Node)rootRateLimiterNode, !matchAllNodesInTree);
    }

    private Node<NodeData<RateLimiter<K>>> toRateLimiterNode(Node<NodeData<Rates>> root) {

        BiFunction<String, NodeData<Rates>, NodeData<RateLimiter<K>>> nodeValueConverter =
                (nodeName, nodeData) -> toRateLimiterNode(root, nodeName, nodeData);

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeData<RateLimiter<K>>> rateLimiterRootNode = root.transform(null, nodeValueConverter);

        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        return rateLimiterRootNode;
    }

    private NodeData<RateLimiter<K>> toRateLimiterNode(
            Node<NodeData<Rates>> root, String nodeName, NodeData<Rates> nodeData) {

        if (NodeUtil.isEqual(root, nodeName, nodeData)) {
            return new NodeData<>(nodeData.getSource(), RateLimiter.noop());
        }

        final Rates rates = nodeData.getValue();

        // One method with 3 @RateLimit annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!rates.hasLimits()) { // This is a group node

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            return new NodeData<>(nodeData.getSource(), RateLimiter.noop());
        }

        final RateLimiterConfig rateLimiterConfig = registries.configs().getOrDefault(nodeName);

        final RateLimiter<K> rateLimiter = registries.factories().getOrDefault(nodeName)
                .createRateLimiter(rateLimiterConfig, rates);

        return new NodeData<>(nodeData.getSource(), rateLimiter);
    }
}
