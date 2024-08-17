package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.registry.Registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class DefaultWebRateLimiterRegistry implements WebRateLimiterRegistry {

    private final Map<String, List<Matcher<RequestInfo>>> matchers;
    private final RateLimiterRegistry<RequestInfo> delegate;

    DefaultWebRateLimiterRegistry(WebRateLimiterContext webRateLimiterContext) {
        this.matchers = new ConcurrentHashMap<>();

        final Registry<Matcher<RequestInfo>> matcherRegistry = Registry.ofDefaults();

        // Collect user defined config
        webRateLimiterContext.getConfigurerOptional()
                .ifPresent(configurer -> configurer.configureMatchers(matcherRegistry));

        // Compose existing and user defined
        final MatcherProvider<RequestInfo> matcherProvider = new MatcherProviderMultiSource(
                webRateLimiterContext.getMatcherProvider(),
                matcherRegistry,
                (name, matcher) -> {
                    final List<Matcher<RequestInfo>> list = matchers
                            .computeIfAbsent(name, k -> new ArrayList<>());
                    if (!list.contains(matcher)) {
                        list.add(matcher);
                    }
                });

        this.delegate = RateLimiterRegistries.of(
                webRateLimiterContext.withMatcherProvider(matcherProvider));
    }

    @Override public boolean isWithinLimit(RequestInfo httpServletRequest) {
        return delegate.isWithinLimit(httpServletRequest);
    }

    @Override public boolean tryAcquire(RequestInfo httpServletRequest, int permits,
            long timeout, TimeUnit timeUnit) {
        return delegate.tryAcquire(httpServletRequest, permits, timeout, timeUnit);
    }

    @Override public MatchContext<RequestInfo> getMatchContextOrDefault(
            String id, MatchContext<RequestInfo> resultIfNone) {
        return delegate.getMatchContextOrDefault(id, resultIfNone);
    }

    @Override public RateLimiterRegistry<RequestInfo> deregister(String id) {
        return delegate.deregister(id);
    }

    @Override public RateLimiterRegistry<RequestInfo> register(RateSource rateSource) {
        return delegate.register(rateSource);
    }

    @Override public RateLimiter getRateLimiterOrDefault(RequestInfo requestInfo,
            RateLimiter resultIfNone) {
        return delegate.getRateLimiterOrDefault(requestInfo, resultIfNone);
    }

    @Override public RateLimiter getRateLimiterOrDefault(RateSource rateSource,
            RateLimiter resultIfNone) {
        return delegate.getRateLimiterOrDefault(rateSource, resultIfNone);
    }

    @Override public boolean isRegistered(String name) {
        return delegate.isRegistered(name);
    }

    @Override
    public boolean hasMatcher(String id) {
        return getMatchers(id).stream().anyMatch(matcher -> !Matchers.matchNone().equals(matcher));
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     */
    private List<Matcher<RequestInfo>> getMatchers(String id) {
        List<Matcher<RequestInfo>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }
}
