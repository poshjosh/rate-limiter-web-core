package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.PatternMatchingRateLimiter;
import com.looseboxes.ratelimiter.rates.Limit;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.web.core.NodeFactory;
import com.looseboxes.ratelimiter.web.core.PatternMatchingRateLimiterFactory;
import com.looseboxes.ratelimiter.web.core.RateLimiterRegistry;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class DefaultPatternMatchingRateLimiterFactory<R, K, S>
        implements PatternMatchingRateLimiterFactory<R> {

    private static final Logger LOG = LoggerFactory.getLogger(
            DefaultPatternMatchingRateLimiterFactory.class);

    private final S sourceOfRateLimitRules;
    private final NodeFactory<S, Limit> nodeFactory;
    private final RateLimiterRegistry<R> rateLimiterRegistry;
    private final BiConsumer<Object, Node<NodeData<Limit>>> defaultNodeConsumer;

    public DefaultPatternMatchingRateLimiterFactory(
            S sourceOfRateLimitRules,
            NodeFactory<S, Limit> nodeFactory,
            RateLimiterRegistry<R> rateLimiterRegistry,
            BiConsumer<Object, Node<NodeData<Limit>>> defaultNodeConsumer) {
        this.sourceOfRateLimitRules = Objects.requireNonNull(sourceOfRateLimitRules);
        this.nodeFactory = Objects.requireNonNull(nodeFactory);
        this.rateLimiterRegistry = Objects.requireNonNull(rateLimiterRegistry);
        this.defaultNodeConsumer = Objects.requireNonNull(defaultNodeConsumer);
    }

    @Override
    public RateLimiter<R> createRateLimiter(String rootNodeName,
            BiConsumer<Object, Node<NodeData<Limit>>> nodeConsumer) {

        PatternMatchingRateLimiter.MatcherProvider<R> matcherProvider =
                (name, nodeData) -> rateLimiterRegistry.getMatcherOrDefault(name, nodeData.getSource());

        Node<NodeData<Limit>> rootNode =
                nodeFactory.createNode(
                        rootNodeName,
                        sourceOfRateLimitRules,
                        nodeConsumer.andThen(defaultNodeConsumer));

        Node<NodeData<RateLimiter<K>>> rootRateLimiterNode = toRateLimiterNode(rootNode);

        boolean matchAllNodesInTree = sourceOfRateLimitRules instanceof RateLimitProperties;

        return new PatternMatchingRateLimiter<>(matcherProvider, (Node)rootRateLimiterNode, !matchAllNodesInTree);
    }

    private Node<NodeData<RateLimiter<K>>> toRateLimiterNode(
            Node<NodeData<Limit>> source) {

        final BiPredicate<String, NodeData<Limit>> isRoot =
                (nodeName, nodeData) -> NodeUtil.isEqual(source, nodeName, nodeData);

        BiFunction<String, NodeData<Limit>, NodeData<RateLimiter<K>>> nodeValueConverter =
                (nodeName, nodeData) -> {

                    if (isRoot.test(nodeName, nodeData)) {
                        return new NodeData<>(nodeData.getSource(), RateLimiter.noop());
                    }else{
                        return convertToRateLimiterNode(nodeName, nodeData);
                    }
                };

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeData<RateLimiter<K>>> rateLimiterRootNode = source
                .transform(null, nodeValueConverter);

        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        return rateLimiterRootNode;
    }

    private NodeData<RateLimiter<K>> convertToRateLimiterNode(
            String name, NodeData<Limit> nodeData) {

        Limit limit = nodeData.getValue();

        // One method with 3 @RateLimit annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!limit.hasLimits()) { // This is a group node

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            return new NodeData<>(nodeData.getSource(), RateLimiter.noop());

        }else {

            RateLimiterConfig rateLimiterConfig = rateLimiterRegistry.getRateLimiterConfig(name);

            RateLimiter<K> rateLimiter = rateLimiterRegistry
                    .getRateLimiterFactory(name)
                    .createRateLimiter(rateLimiterConfig, limit);

            return new NodeData<>(nodeData.getSource(), rateLimiter);
        }
    }
}
