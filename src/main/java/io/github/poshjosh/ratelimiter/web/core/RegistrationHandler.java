package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.RateConfig;
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

    RegistrationHandler(Registries<R> registries, MatcherFactory<R> matcherFactory) {
        this.registries = Objects.requireNonNull(registries);
        this.matcherFactory = Objects.requireNonNull(matcherFactory);
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

        Optional<Matcher<R, ?>> createdMatcherOpt = createMatcher(node);

        if (existingMatcher == Matcher.MATCH_NONE) {
            createdMatcherOpt.ifPresent(matcher -> {
                registries.matchers().register(nodeName, matcher);
            });
        } else {
            LOG.debug("Found existing matcher for {}", nodeName);
            Matcher<R, ?> createdMatcher = createdMatcherOpt.orElse(null);
            registries.matchers().register(nodeName, createdMatcher == null ? existingMatcher :
                    createdMatcher.andThen((Matcher) existingMatcher));
        }
    }

    private void noop(String nodeName) {
        registries.matchers().register(nodeName, Matcher.matchNone());
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

    private Rates requireRates(Node<RateConfig> node) {
        return Objects.requireNonNull(Checks.requireNodeValue(node).getValue());
    }
}
