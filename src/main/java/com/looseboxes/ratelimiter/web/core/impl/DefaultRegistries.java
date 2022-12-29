package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.annotations.Nullable;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.ResourceLimiterConfigurer;
import com.looseboxes.ratelimiter.web.core.Registries;
import com.looseboxes.ratelimiter.web.core.Registry;
import com.looseboxes.ratelimiter.web.core.ResourceLimiterFactory;

import java.lang.reflect.Method;

final class DefaultRegistries<R> implements Registries<R> {

    private final Registry<Matcher<R, ?>> matcherRegistry;

    private final Registry<ResourceLimiterConfig<?, ?>> rateLimiterConfigRegistry;

    private final Registry<ResourceLimiterFactory<?>> rateLimiterFactoryRegistry;

    DefaultRegistries(
            IdProvider<Class<?>, String> classIdProvider,
            IdProvider<Method, String> methodIdProvider,
            ResourceLimiterConfig<?, ?> resourceLimiterConfig,
            ResourceLimiterFactory<?> resourceLimiterFactory,
            @Nullable ResourceLimiterConfigurer<R> resourceLimiterConfigurer) {
        this.matcherRegistry = SimpleRegistry.of(Matcher.matchNone(), classIdProvider, methodIdProvider);
        this.rateLimiterConfigRegistry = SimpleRegistry.of(resourceLimiterConfig, classIdProvider, methodIdProvider);
        this.rateLimiterFactoryRegistry = SimpleRegistry.of(resourceLimiterFactory, classIdProvider, methodIdProvider);
        if(resourceLimiterConfigurer != null) {
            resourceLimiterConfigurer.configure(this);
        }
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
            return ResourceLimiterConfig.of();
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
            return ResourceLimiterConfig.of();
        }
    }

    public <K, V> Registry<RateCache<K, V>> caches() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new CacheProxy<K, V>());
    }

    public Registry<ResourceUsageListener> listeners() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new UsageListenerProxy<>());
    }
}
