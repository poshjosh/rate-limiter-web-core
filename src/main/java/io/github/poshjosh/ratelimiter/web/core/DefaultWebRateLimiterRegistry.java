package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

final class DefaultWebRateLimiterRegistry implements WebRateLimiterRegistry {

    private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
    private final Registries registries;

    private final RateLimitProperties properties;
    private final RateLimiterRegistry<HttpServletRequest> delegate;

    DefaultWebRateLimiterRegistry(WebRateLimiterContext webRateLimiterContext) {
        this.matchers = new ConcurrentHashMap<>();
        this.registries = Registries.ofDefaults();

        // Collect user defined config
        webRateLimiterContext.getConfigurerOptional()
                .ifPresent(configurer -> configurer.configure(this.registries));

        // Compose existing config and user defined config
        MatcherProvider<HttpServletRequest> matcherProvider = new MatcherProviderMultiSource(
                webRateLimiterContext.getMatcherProvider(),
                this.registries.matchers(),
                new MatcherCollector(this.matchers));

        // Add composed config to context
        webRateLimiterContext = (WebRateLimiterContext)webRateLimiterContext
                .withMatcherProvider(matcherProvider);

        this.properties = webRateLimiterContext.getProperties();

        this.delegate = RateLimiterRegistry.of(webRateLimiterContext);
    }

    @Override public RateLimiterRegistry<HttpServletRequest> register(Class<?> source) {
        return delegate.register(source);
    }

    @Override public RateLimiterRegistry<HttpServletRequest> register(Method source) {
        return delegate.register(source);
    }

    @Override public RateLimiterFactory<HttpServletRequest> createRateLimiterFactory() {
        return delegate.createRateLimiterFactory();
    }

    @Override public Optional<RateLimiter> getRateLimiter(Class<?> clazz) {
        return delegate.getRateLimiter(clazz);
    }

    @Override public Optional<RateLimiter> getRateLimiter(Method method) {
        return delegate.getRateLimiter(method);
    }

    @Override public boolean isRegistered(String name) {
        return delegate.isRegistered(name);
    }

    @Override
    public boolean hasMatching(String id) {
        return getMatchers(id).stream().anyMatch(matcher -> !Matcher.matchNone().equals(matcher));
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     */
    private List<Matcher<HttpServletRequest>> getMatchers(String id) {
        List<Matcher<HttpServletRequest>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }

    @Override
    public UnmodifiableRegistries registries() {
        return Registries.unmodifiable(registries);
    }

    private static final class MatcherCollector implements BiConsumer<String, Matcher<HttpServletRequest>> {
        private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
        private MatcherCollector(Map<String, List<Matcher<HttpServletRequest>>> matchers) {
            this.matchers = Objects.requireNonNull(matchers);
        }
        @Override public void accept(String name, Matcher<HttpServletRequest> matcher) {
            List<Matcher<HttpServletRequest>> list = matchers.computeIfAbsent(name, k -> new ArrayList<>());
            if (!list.contains(matcher)) {
                list.add(matcher);
            }
        }
    }
}
