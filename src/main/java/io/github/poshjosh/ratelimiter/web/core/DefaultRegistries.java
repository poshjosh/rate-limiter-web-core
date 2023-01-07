package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.util.Matcher;

final class DefaultRegistries<R> implements Registries<R> {

    private final Registry<ResourceLimiter<?>> resourceLimiterRegistry;

    private final Registry<Matcher<R, ?>> matcherRegistry;

    private final Registry<RateCache<?>> cacheRegistry;

    private final Registry<UsageListener> listenerRegistry;

    DefaultRegistries(ResourceLimiter<?> resourceLimiter, Matcher<R, ?> matcher,
                      RateCache<?> rateCache, UsageListener usageListener) {
        this.resourceLimiterRegistry = Registry.of(resourceLimiter);
        this.matcherRegistry = Registry.of(matcher);
        this.cacheRegistry = Registry.of(rateCache);
        this.listenerRegistry = Registry.of(usageListener);
    }

    @Override
    public Registry<ResourceLimiter<?>> limiters() {
        return resourceLimiterRegistry;
    }

    @Override
    public Registry<Matcher<R, ?>> matchers() {
        return matcherRegistry;
    }

    @Override
    public Registry<RateCache<?>> caches() {
        return cacheRegistry;
    }

    @Override
    public Registry<UsageListener> listeners() {
        return listenerRegistry;
    }

    public String toString() {
        return "DefaultRegistries{\nmatcherRegistry=" + matcherRegistry +
                "\nlimiterRegistry=" + resourceLimiterRegistry + "}";
    }
}
