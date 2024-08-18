package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.Matchers;
import io.github.poshjosh.ratelimiter.web.core.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

final class MatcherProviderMultiSource implements MatcherProvider<RequestInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(MatcherProviderMultiSource.class);
    private final MatcherProvider<RequestInfo> delegate;
    private final Registry<Matcher<RequestInfo>> registry;
    private final BiConsumer<String, Matcher<RequestInfo>> onMatcherCreated;

    MatcherProviderMultiSource(
            MatcherProvider<RequestInfo> delegate,
            Registry<Matcher<RequestInfo>> registry,
            BiConsumer<String, Matcher<RequestInfo>> onMatcherCreated) {
        this.delegate = Objects.requireNonNull(delegate);
        this.registry = Objects.requireNonNull(registry);
        this.onMatcherCreated = Objects.requireNonNull(onMatcherCreated);
    }
    @Override
    public Matcher<RequestInfo> createMainMatcher(RateConfig rateConfig) {

        final String id = rateConfig.getId();

        final Matcher<RequestInfo> registered = registry.getOrDefault(id, null);
        LOG.debug("Found registered for {}, matcher: {}", id, registered);
        final boolean noneRegistered = registered == null || Matchers.matchNone().equals(registered);

        final Matcher<RequestInfo> created = delegate.createMainMatcher(rateConfig);
        final boolean noneCreated = created == null || Matchers.matchNone().equals(created);

        if (noneRegistered && noneCreated) {
            LOG.debug("No matcher for {}", id);
            return Matchers.matchNone();
        }

        if (noneRegistered) {
            onMatcherCreated.accept(id, created);
            return created;
        }

        Matcher<RequestInfo> result = noneCreated ? registered : registered.and(created);
        onMatcherCreated.accept(id, result);
        return result;
    }
    @Override
    public List<Matcher<RequestInfo>> createSubMatchers(RateConfig rateConfig) {
        List<Matcher<RequestInfo>> result = delegate.createSubMatchers(rateConfig);
        result.forEach(matcher -> onMatcherCreated.accept(rateConfig.getId(), matcher));
        return result;
    }
}
