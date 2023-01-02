package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

final class DefaultRegistries<R> implements Registries<R> {

    private final AccessibleRegistry<ResourceLimiter<?>> resourceLimiterRegistry;

    private final AccessibleRegistry<Matcher<R, ?>> matcherRegistry;

    private final AccessibleRegistry<ResourceLimiterFactory<?>> rateLimiterFactoryRegistry;

    DefaultRegistries(
            ResourceLimiter<?> resourceLimiter,
            Matcher<R, ?> matcher,
            ResourceLimiterFactory<?> resourceLimiterFactory) {
        this.resourceLimiterRegistry = SimpleAccessibleRegistry.of(resourceLimiter);
        this.matcherRegistry = SimpleAccessibleRegistry.of(matcher);
        this.rateLimiterFactoryRegistry = SimpleAccessibleRegistry.of(resourceLimiterFactory);
    }

    @Override
    public AccessibleRegistry<ResourceLimiter<?>> limiters() {
        return resourceLimiterRegistry;
    }

    @Override
    public AccessibleRegistry<Matcher<R, ?>> matchers() {
        return matcherRegistry;
    }

    @Override
    public AccessibleRegistry<ResourceLimiterFactory<?>> factories() {
        return rateLimiterFactoryRegistry;
    }

    private static final class CacheProxy<K, V> implements ProxyRegistry.Proxy<ResourceLimiter<?>, RateCache<K, V>> {
        @Override public ResourceLimiter<?> set(ResourceLimiter<?> input, RateCache<K, V> output) {
            return input.cache((RateCache)output);
        }
    }

    private static final class UsageListenerProxy implements ProxyRegistry.Proxy<ResourceLimiter<?>, ResourceUsageListener> {
        @Override public ResourceLimiter<?> set(ResourceLimiter<?> input, ResourceUsageListener output) {
            return input.listener(output);
        }
    }

    public <K, V> Registry<RateCache<K, V>> caches() {
        return new ProxyRegistry(resourceLimiterRegistry, new CacheProxy<K, V>());
    }

    public Registry<ResourceUsageListener> listeners() {
        return new ProxyRegistry(resourceLimiterRegistry, new UsageListenerProxy());
    }
}
