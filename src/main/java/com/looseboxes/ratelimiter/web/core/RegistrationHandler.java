package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

final class RegistrationHandler<R>{

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandler.class);

    private final Registries<R> registries;
    private final MatcherFactory<R> matcherFactory;
    private final ResourceLimiterFactory<Object> resourceLimiterFactory;

    RegistrationHandler(Registries<R> registries,
                     MatcherFactory<R> matcherFactory,
                     ResourceLimiterFactory<Object> resourceLimiterFactory) {
        this.registries = Objects.requireNonNull(registries);
        this.matcherFactory = Objects.requireNonNull(matcherFactory);
        this.resourceLimiterFactory = Objects.requireNonNull(resourceLimiterFactory);
    }

    public void registerMatchersAndRateLimiters(Node<RateConfig> root) {

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
            Node<RateConfig> root, String nodeName, RateConfig rateConfig) {

        if (isEqual(root, nodeName, rateConfig)) {
            noop(nodeName);
        }

        final Rates rates = rateConfig.getValue();

        // One method with 3 @Rate annotations is a simple group (not really a group)
        // A true group spans either multiple methods/classes
        if(!rates.hasLimits()) { // This is a group node

            // @TODO how do we handle this?
            // Do we create multiple rate limiters, one for each of the direct children of this group
            // Do we re-use the rate limiters of the children ? They must already exist since we create children first
            noop(nodeName);
        }

        if (!registries.matchers().get(nodeName).isPresent()) {
            createMatcher(nodeName, rateConfig).ifPresent(matcher -> {
                registries.matchers().register(nodeName, matcher);
            });
        } else {
            LOG.debug("Found existing matcher for {}", nodeName);
        }

        if (!registries.limiters().get(nodeName).isPresent()) {
            createLimiter(nodeName, rateConfig.getValue()).ifPresent(limiter -> {
                registries.limiters().register(nodeName, limiter);
            });
        } else {
            LOG.debug("Found existing limiter for {}", nodeName);
        }
    }

    private void noop(String nodeName) {
        registries.matchers().register(nodeName, Matcher.matchNone());
        registries.limiters().register(nodeName, ResourceLimiter.noop());
    }

    private boolean isEqual(Node<RateConfig> node, String name, RateConfig rateConfig) {
        return Objects.equals(node.getName(), name) && Objects.equals(node.getValueOrDefault(null),
                rateConfig);
    }

    private Optional<Matcher<R, ?>> createMatcher(String name, RateConfig rateConfig) {
        if (rateConfig == null) {
            return Optional.empty();
        }
        return matcherFactory.createMatcher(name, rateConfig);
    }

    private Optional<ResourceLimiter<?>> createLimiter(String name, Rates rates) {
        if (rates == null) {
            return Optional.empty();
        }
        ResourceLimiter<Object> resourceLimiter = resourceLimiterFactory.createNew(rates);
        if (resourceLimiter == null) {
            return Optional.empty();
        }
        resourceLimiter = resourceLimiter
                .cache(registries.caches().getOrDefault(name))
                .listener(registries.listeners().getOrDefault(name));
        return Optional.of(resourceLimiter);
    }
}
