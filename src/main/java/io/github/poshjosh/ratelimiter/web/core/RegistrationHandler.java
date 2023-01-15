package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
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
        LOG.debug("Nodes:\n{}", root);
        root.visitAll(node -> {
            node.getValueOptional().ifPresent(v -> registerMatcherAndRateLimiter(root, node));
        });
    }

    private void registerMatcherAndRateLimiter(Node<RateConfig> root, Node<RateConfig> node) {

        final String nodeName = node.getName();
        final RateConfig rateConfig = node.getValueOrDefault(null);

        if (isEqual(root, nodeName, rateConfig)) {
            noop(nodeName);
            return;
        }

        LOG.trace("Processing: {} = {}", nodeName, rateConfig);

        // If no Matcher or a NO_OP Matcher exists, create new
        Matcher<R, ?> existingMatcher = registries.matchers()
                .get(nodeName).orElse(Matcher.matchNone());

        if (existingMatcher == Matcher.MATCH_NONE) {
            createMatcher(node).ifPresent(matcher -> {
                registries.matchers().register(nodeName, matcher);
            });
        } else {
            LOG.debug("Found existing matcher for {}", nodeName);
        }

        // If no Limiter or a NO_OP Limiter exists, create new
        ResourceLimiter<?> existingLimiter = registries.limiters()
                .get(nodeName).orElse(ResourceLimiter.NO_OP);
        if (existingLimiter == ResourceLimiter.NO_OP) {
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
        return Objects.equals(node.getName(), name)
                && Objects.equals(node.getValueOrDefault(null), rateConfig);
    }

    private Optional<Matcher<R, ?>> createMatcher(Node<RateConfig> node) {
        if(!requireRates(node).hasLimits() && !parentHasLimits(node)) {
            LOG.debug("No limits specified for group, so no matcher will be registered for: {}",
                    node.getName());
            return Optional.of(Matcher.matchNone());
        }
        return matcherFactory.createMatcher(node.getName(), Checks.requireNodeValue(node));
    }

    private boolean parentHasLimits(Node<RateConfig> node) {
        return node.getParentOptional()
                .filter(parent -> parent.hasNodeValue() && requireRates(parent).hasLimits())
                .isPresent();
    }

    private Optional<ResourceLimiter<?>> createLimiter(String name, Rates rates) {
        if (!rates.hasLimits()) {
            LOG.debug("No limits at node, so no matcher will be registered for: {}", name);
            return Optional.of(ResourceLimiter.noop());
        }
        RateCache cache = registries.caches().getOrDefault(name);
        UsageListener listener = registries.listeners().getOrDefault(name);
        ResourceLimiter<?> resourceLimiter = resourceLimiterFactory
                .createNew(cache, listener, rates);
        return Optional.ofNullable(resourceLimiter);
    }

    private Rates requireRates(Node<RateConfig> node) {
        return Objects.requireNonNull(Checks.requireNodeValue(node).getValue());
    }
}
