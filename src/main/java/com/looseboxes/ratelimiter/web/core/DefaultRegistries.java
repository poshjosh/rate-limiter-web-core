package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

final class DefaultRegistries<R> implements Registries<R> {

    private final Registry<ResourceLimiter<?>> resourceLimiterRegistry;

    private final Registry<Matcher<R, ?>> matcherRegistry;

    private final Registry<ResourceLimiterConfig<?, ?>> rateLimiterConfigRegistry;

    private final Registry<ResourceLimiterFactory<?>> rateLimiterFactoryRegistry;

    DefaultRegistries(
            ResourceLimiter<?> resourceLimiter,
            Matcher<R, ?> matcher,
            ResourceLimiterConfig<?, ?> resourceLimiterConfig,
            ResourceLimiterFactory<?> resourceLimiterFactory) {
        this.resourceLimiterRegistry = SimpleRegistry.of(resourceLimiter);
        this.matcherRegistry = SimpleRegistry.of(matcher);
        this.rateLimiterConfigRegistry = SimpleRegistry.of(resourceLimiterConfig);
        this.rateLimiterFactoryRegistry = SimpleRegistry.of(resourceLimiterFactory);
    }

    @Override
    public Registry<ResourceLimiter<?>> resourceLimiters() {
        return resourceLimiterRegistry;
    }

    @Override
    public Registry<Matcher<R, ?>> matchers() {
        return matcherRegistry;
    }

    @Override
    public Registry<ResourceLimiterFactory<?>> factories() {
        return rateLimiterFactoryRegistry;
    }

    @Override
    public Registry<ResourceLimiterConfig<?, ?>> configs() {
        return rateLimiterConfigRegistry;
    }

    private static final class CacheProxy<K, V> implements ProxyRegistry.Proxy<ResourceLimiterConfig<K, V>, RateCache<K, V>> {
        @Override public RateCache<K, V> get(ResourceLimiterConfig<K, V> input) {
            return input.getCache();
        }
        @Override public ResourceLimiterConfig<K, V> set(ResourceLimiterConfig<K, V> input, RateCache<K, V> output) {
            return ResourceLimiterConfig.builder(input).cache(output).build();
        }
        @Override public ResourceLimiterConfig<K, V> createNewProxied() {
            return ResourceLimiterConfig.ofDefaults();
        }
    }

    private static final class UsageListenerProxy<K, V> implements ProxyRegistry.Proxy<ResourceLimiterConfig<K, V>, ResourceUsageListener> {
        @Override public ResourceUsageListener get(ResourceLimiterConfig<K, V> input) {
            return input.getUsageListener();
        }
        @Override public ResourceLimiterConfig<K, V> set(ResourceLimiterConfig<K, V> input, ResourceUsageListener output) {
            return ResourceLimiterConfig.builder(input).usageListener(output).build();
        }
        @Override public ResourceLimiterConfig<K, V> createNewProxied() {
            return ResourceLimiterConfig.ofDefaults();
        }
    }

    public <K, V> Registry<RateCache<K, V>> caches() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new CacheProxy<K, V>());
    }

    public Registry<ResourceUsageListener> listeners() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new UsageListenerProxy<>());
    }
}
