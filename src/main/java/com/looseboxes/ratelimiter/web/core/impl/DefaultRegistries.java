package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.MatcherRegistry;
import com.looseboxes.ratelimiter.web.core.RateLimiterConfigurer;
import com.looseboxes.ratelimiter.web.core.Registries;
import com.looseboxes.ratelimiter.web.core.Registry;

import java.lang.reflect.Method;
import java.util.*;

final class DefaultRegistries<R> implements Registries<R> {

    private final MatcherRegistry<R> matcherRegistry;

    private final Registry<RateLimiterConfig<?, ?>> rateLimiterConfigRegistry;

    private final Registry<RateLimiterFactory<?>> rateLimiterFactoryRegistry;

    DefaultRegistries(
            IdProvider<Class<?>, String> classIdProvider,
            IdProvider<Method, String> methodIdProvider,
            MatcherRegistry<R> matcherRegistry,
            RateLimiterConfig<?, ?> rateLimiterConfig,
            RateLimiterFactory<?> rateLimiterFactory,
            @Nullable RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.matcherRegistry = Objects.requireNonNull(matcherRegistry);
        this.rateLimiterConfigRegistry = SimpleRegistry.of(rateLimiterConfig, classIdProvider, methodIdProvider);
        this.rateLimiterFactoryRegistry = SimpleRegistry.of(rateLimiterFactory, classIdProvider, methodIdProvider);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
    }

    @Override
    public MatcherRegistry<R> matchers() {
        return matcherRegistry;
    }

    @Override
    public Registry<RateLimiterFactory<?>> factories() {
        return rateLimiterFactoryRegistry;
    }

    @Override
    public Registry<RateLimiterConfig<?, ?>> configs() {
        return rateLimiterConfigRegistry;
    }

    private static final class RateCacheProxy<K, V> implements ProxyRegistry.Proxy<RateLimiterConfig<K, V>, RateCache<K, V>> {
        @Override public RateCache<K, V> get(RateLimiterConfig<K, V> input) {
            return input.getRateCache();
        }
        @Override public RateLimiterConfig<K, V> set(RateLimiterConfig<K, V> input, RateCache<K, V> output) {
            return RateLimiterConfig.builder(input).rateCache(output).build();
        }
        @Override public RateLimiterConfig<K, V> newInstance() {
            return RateLimiterConfig.newInstance();
        }
    }

    private static final class RateRecordedListenerProxy<K, V> implements ProxyRegistry.Proxy<RateLimiterConfig<K, V>, RateRecordedListener> {
        @Override public RateRecordedListener get(RateLimiterConfig<K, V> input) {
            return input.getRateRecordedListener();
        }
        @Override public RateLimiterConfig<K, V> set(RateLimiterConfig<K, V> input, RateRecordedListener output) {
            return RateLimiterConfig.builder(input).rateRecordedListener(output).build();
        }
        @Override public RateLimiterConfig<K, V> newInstance() {
            return RateLimiterConfig.newInstance();
        }
    }

    public <K, V> Registry<RateCache<K, V>> caches() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new RateCacheProxy<K, V>());
    }

    public Registry<RateRecordedListener> listeners() {
        return new ProxyRegistry(rateLimiterConfigRegistry, new RateRecordedListenerProxy<>());
    }
}
