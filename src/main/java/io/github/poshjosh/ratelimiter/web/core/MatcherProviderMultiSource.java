package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.Matchers;
import io.github.poshjosh.ratelimiter.web.core.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

final class MatcherProviderMultiSource implements MatcherProvider<HttpServletRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(MatcherProviderMultiSource.class);
    private final MatcherProvider<HttpServletRequest> delegate;
    private final Registry<Matcher<HttpServletRequest>> registry;
    private final BiConsumer<String, Matcher<HttpServletRequest>> onMatcherCreated;

    MatcherProviderMultiSource(
            MatcherProvider<HttpServletRequest> delegate,
            Registry<Matcher<HttpServletRequest>> registry,
            BiConsumer<String, Matcher<HttpServletRequest>> onMatcherCreated) {
        this.delegate = Objects.requireNonNull(delegate);
        this.registry = Objects.requireNonNull(registry);
        this.onMatcherCreated = Objects.requireNonNull(onMatcherCreated);
    }
    @Override
    public Matcher<HttpServletRequest> createMainMatcher(RateConfig rateConfig) {

        final String id = rateConfig.getId();

        final Matcher<HttpServletRequest> registered = registry.getOrDefault(id, null);
        LOG.debug("Found registered for {}, matcher: {}", id, registered);
        final boolean noneRegistered = registered == null || Matchers.matchNone().equals(registered);

        final Matcher<HttpServletRequest> created = delegate.createMainMatcher(rateConfig);
        final boolean noneCreated = created == null || Matchers.matchNone().equals(created);

        if (noneRegistered && noneCreated) {
            LOG.debug("No matcher for {}", id);
            return Matchers.matchNone();
        }

        if (noneRegistered) {
            onMatcherCreated.accept(id, created);
            return created;
        }

        Matcher<HttpServletRequest> result = noneCreated ? registered : registered.and(created);
        onMatcherCreated.accept(id, result);
        return result;
    }
    @Override
    public List<Matcher<HttpServletRequest>> createLimitMatchers(RateConfig rateConfig) {
        List<Matcher<HttpServletRequest>> result = delegate.createLimitMatchers(rateConfig);
        result.forEach(matcher -> onMatcherCreated.accept(rateConfig.getId(), matcher));
        return result;
    }
}
