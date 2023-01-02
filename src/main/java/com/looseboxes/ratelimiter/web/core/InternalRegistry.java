package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

final class InternalRegistry<R, S>{

    private static final Logger LOG = LoggerFactory.getLogger(InternalRegistry.class);

    private final Registries<R> registries;
    private final MatcherFactory<R, S> matcherFactory;

    InternalRegistry(Registries<R> registries, MatcherFactory<R, S> matcherFactory) {
        this.registries = Objects.requireNonNull(registries);
        this.matcherFactory = Objects.requireNonNull(matcherFactory);
    }

    public void registerMatchersAndRateLimiters(Node<NodeValue<Rates>> root) {

        root.visitAll(node -> {
            node.getValueOptional().ifPresent(nodeValue -> {
                registerMatcherAndRateLimiter(root, node.getName(), nodeValue);
            });
        });

        if(LOG.isDebugEnabled()) {
            LOG.debug("ResourceLimiter nodes: {}", NodeFormatter.indentedHeirarchy().format(root));
        }
    }

    private void registerMatcherAndRateLimiter(
            Node<NodeValue<Rates>> root, String nodeName, NodeValue<Rates> nodeValue) {

        if (isEqual(root, nodeName, nodeValue)) {
            noop(nodeName);
        }

        final Rates rates = nodeValue.getValue();

        // One method with 3 @RateLimit annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!rates.hasLimits()) { // This is a group node

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            noop(nodeName);
        }

        createMatcher(nodeName, nodeValue.getSource())
                .ifPresent(matcher -> registries.matchers().register(nodeName, matcher));

        final ResourceLimiter resourceLimiter = registries.factories()
                .getOrDefault(nodeName).createNew(rates);

        registries.limiters().register(nodeName, resourceLimiter);
    }

    private void noop(String nodeName) {
        registries.matchers().register(nodeName, Matcher.matchNone());
        registries.limiters().register(nodeName, ResourceLimiter.noop());
    }

    private boolean isEqual(Node<NodeValue<Rates>> node, String name, NodeValue<Rates> nodeValue) {
        return Objects.equals(node.getName(), name) && Objects.equals(node.getValueOrDefault(null), nodeValue);
    }

    private Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
        if (source == null) {
            return Optional.empty();
        }
        Matcher<R, ?> registeredMatcher = registries.matchers().getOrDefault(name, null);
        if (registeredMatcher == null) {
            return matcherFactory.createMatcher(name, (S)source);
        }
        Matcher<R, ?> createdMatcher = matcherFactory.createMatcher(name, (S)source).orElse(null);
        if (createdMatcher == null) {
            return Optional.empty();
        }
        return Optional.of(registeredMatcher.andThen((Matcher)createdMatcher));
    }
}
