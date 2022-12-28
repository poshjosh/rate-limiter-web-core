package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.annotations.Nullable;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

final class PatternMatchingRateLimiterFactory<R, K>{

    private static final Logger LOG = LoggerFactory.getLogger(PatternMatchingRateLimiterFactory.class);

    private final Node<NodeValue<Rates>> rootNode;
    private final Registries<R> registries;
    private final MatcherFactory<R, Object> matcherFactory;
    private final boolean firstMatchOnly;

    PatternMatchingRateLimiterFactory(
            Node<NodeValue<Rates>> rootNode, Registries<R> registries,
            MatcherFactory<R, Object> matcherFactory, boolean firstMatchOnly) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.registries = Objects.requireNonNull(registries);
        this.matcherFactory = Objects.requireNonNull(matcherFactory);
        this.firstMatchOnly = firstMatchOnly;
    }

    public RateLimiter<R> createRateLimiter() {

        PatternMatchingRateLimiter.MatcherProvider<R> matcherProvider =
                (name, nodeData) -> registries.matchers().getOrDefault(name);

        Node<NodeValue<RateLimiter<K>>> rootRateLimiterNode = toRateLimiterNode(rootNode);

        return new PatternMatchingRateLimiter<>(matcherProvider, (Node)rootRateLimiterNode, firstMatchOnly);
    }

    private Node<NodeValue<RateLimiter<K>>> toRateLimiterNode(Node<NodeValue<Rates>> root) {

        BiFunction<String, NodeValue<Rates>, NodeValue<RateLimiter<K>>> nodeValueConverter =
                (nodeName, nodeData) -> toRateLimiterNode(root, nodeName, nodeData);

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeValue<RateLimiter<K>>> rateLimiterRootNode = root.transform(nodeValueConverter);

        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        return rateLimiterRootNode;
    }

    private NodeValue<RateLimiter<K>> toRateLimiterNode(
            Node<NodeValue<Rates>> root, String nodeName, NodeValue<Rates> nodeValue) {

        if (isEqual(root, nodeName, nodeValue)) {

            registries.matchers().register(nodeName, Matcher.matchNone());

            return NodeValue.of(nodeValue.getSource(), RateLimiter.noop());
        }

        final Rates rates = nodeValue.getValue();

        //System.out.printf("%s PatternMatchingRateLimiterFactory rates: %s\n", java.time.LocalTime.now(), rates);

        // One method with 3 @RateLimit annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!rates.hasLimits()) { // This is a group node

            registries.matchers().register(nodeName, Matcher.matchNone());

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            return NodeValue.of(nodeValue.getSource(), RateLimiter.noop());
        }

        createMatcher(nodeName, nodeValue.getSource())
                .ifPresent(matcher -> registries.matchers().register(nodeName, matcher));

        final RateLimiterConfig rateLimiterConfig = registries.configs().getOrDefault(nodeName);

        final RateLimiter<K> rateLimiter = registries.factories()
                .getOrDefault(nodeName).createNew(rateLimiterConfig, rates);

        return NodeValue.of(nodeValue.getSource(), rateLimiter);
    }

    private boolean isEqual(Node<NodeValue<Rates>> node, String name, NodeValue<Rates> nodeValue) {
        return Objects.equals(node.getName(), name) && Objects.equals(node.getValueOrDefault(null), nodeValue);
    }

    private Optional<Matcher<R, ?>> createMatcher(String name, @Nullable Object source) {
        if (source == null) {
            return Optional.empty();
        }
        Matcher<R, ?> registeredMatcher = registries.matchers().getOrDefault(name, null);
        if (registeredMatcher == null) {
            return matcherFactory.createMatcher(name, source);
        }
        Matcher<R, ?> createdMatcher = matcherFactory.createMatcher(name, source).orElse(null);
        if (createdMatcher == null) {
            return Optional.empty();
        }
        return Optional.of(registeredMatcher.andThen((Matcher)createdMatcher));
    }
}
