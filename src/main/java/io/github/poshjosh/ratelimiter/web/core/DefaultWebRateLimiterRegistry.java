package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.registry.Registry;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class DefaultWebRateLimiterRegistry implements WebRateLimiterRegistry {

    private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
    private final RateLimiterRegistry<HttpServletRequest> delegate;

    DefaultWebRateLimiterRegistry(WebRateLimiterContext webRateLimiterContext) {
        this.matchers = new ConcurrentHashMap<>();

        final Registry<Matcher<HttpServletRequest>> matcherRegistry = Registry.ofDefaults();

        // Collect user defined config
        webRateLimiterContext.getConfigurerOptional()
                .ifPresent(configurer -> configurer.configureMatchers(matcherRegistry));

        // Compose existing and user defined
        final MatcherProvider<HttpServletRequest> matcherProvider = new MatcherProviderMultiSource(
                webRateLimiterContext.getMatcherProvider(),
                matcherRegistry,
                (name, matcher) -> {
                    final List<Matcher<HttpServletRequest>> list = matchers
                            .computeIfAbsent(name, k -> new ArrayList<>());
                    if (!list.contains(matcher)) {
                        list.add(matcher);
                    }
                });

        this.delegate = RateLimiterRegistries.of(
                webRateLimiterContext.withMatcherProvider(matcherProvider));
    }

    @Override public RateLimiterRegistry<HttpServletRequest> register(String id, Rates rates) {
        return delegate.register(id, rates);
    }

    @Override public RateLimiterRegistry<HttpServletRequest> register(Class<?> source) {
        return delegate.register(source);
    }

    @Override public RateLimiterRegistry<HttpServletRequest> register(Method source) {
        return delegate.register(source);
    }

    @Override public Optional<RateLimiter> getClassRateLimiterOptional(Class<?> clazz) {
        return delegate.getClassRateLimiterOptional(clazz);
    }

    @Override public Optional<RateLimiter> getMethodRateLimiterOptional(Method method) {
        return delegate.getMethodRateLimiterOptional(method);
    }

    @Override public Optional<RateLimiter> getRateLimiterOptional(HttpServletRequest request) {
        return delegate.getRateLimiterOptional(request);
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
    private List<Matcher<HttpServletRequest>> getMatchers(String id) {
        List<Matcher<HttpServletRequest>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }
}
