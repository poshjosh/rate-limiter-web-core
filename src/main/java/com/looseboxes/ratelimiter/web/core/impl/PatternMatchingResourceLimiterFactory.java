package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.annotations.Nullable;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

final class PatternMatchingResourceLimiterFactory<R, K>{

    private static final Logger LOG = LoggerFactory.getLogger(PatternMatchingResourceLimiterFactory.class);

    private final Node<NodeValue<Rates>> rootNode;
    private final Registries<R> registries;
    private final MatcherFactory<R, Object> matcherFactory;
    private final boolean firstMatchOnly;

    PatternMatchingResourceLimiterFactory(
            Node<NodeValue<Rates>> rootNode, Registries<R> registries,
            MatcherFactory<R, Object> matcherFactory, boolean firstMatchOnly) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.registries = Objects.requireNonNull(registries);
        this.matcherFactory = Objects.requireNonNull(matcherFactory);
        this.firstMatchOnly = firstMatchOnly;
    }

    public ResourceLimiter<R> createRateLimiter() {

        PatternMatchingResourceLimiter.MatcherProvider<R> matcherProvider =
                (name, nodeData) -> registries.matchers().getOrDefault(name);

        Node<NodeValue<ResourceLimiter<K>>> rootRateLimiterNode = toRateLimiterNode(rootNode);

        return new PatternMatchingResourceLimiter<>(matcherProvider, (Node)rootRateLimiterNode, firstMatchOnly);
    }

    private Node<NodeValue<ResourceLimiter<K>>> toRateLimiterNode(Node<NodeValue<Rates>> root) {

        BiFunction<String, NodeValue<Rates>, NodeValue<ResourceLimiter<K>>> nodeValueConverter =
                (nodeName, nodeData) -> toRateLimiterNode(root, nodeName, nodeData);

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeValue<ResourceLimiter<K>>> rateLimiterRootNode = root.transform(nodeValueConverter);

        if(LOG.isDebugEnabled()) {
            LOG.debug("ResourceLimiter nodes: {}", NodeFormatter.indentedHeirarchy().format(rateLimiterRootNode));
        }

        return rateLimiterRootNode;
    }

    private NodeValue<ResourceLimiter<K>> toRateLimiterNode(
            Node<NodeValue<Rates>> root, String nodeName, NodeValue<Rates> nodeValue) {

        if (isEqual(root, nodeName, nodeValue)) {

            registries.matchers().register(nodeName, Matcher.matchNone());

            return NodeValue.of(nodeValue.getSource(), ResourceLimiter.noop());
        }

        final Rates rates = nodeValue.getValue();

        //System.out.printf("%s PatternMatchingResourceLimiterFactory rates: %s\n", java.time.LocalTime.now(), rates);

        // One method with 3 @RateLimit annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!rates.hasLimits()) { // This is a group node

            registries.matchers().register(nodeName, Matcher.matchNone());

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            return NodeValue.of(nodeValue.getSource(), ResourceLimiter.noop());
        }

        createMatcher(nodeName, nodeValue.getSource())
                .ifPresent(matcher -> registries.matchers().register(nodeName, matcher));

        final ResourceLimiterConfig resourceLimiterConfig = registries.configs().getOrDefault(nodeName);

        final ResourceLimiter<K> resourceLimiter = registries.factories()
                .getOrDefault(nodeName).createNew(resourceLimiterConfig, rates);

        return NodeValue.of(nodeValue.getSource(), resourceLimiter);
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
