package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.MatcherRegistry;
import com.looseboxes.ratelimiter.web.core.RateLimiterConfigurer;
import com.looseboxes.ratelimiter.web.core.RateLimiterRegistry;
import com.looseboxes.ratelimiter.util.Matcher;

import java.lang.reflect.Method;
import java.util.*;

public class DefaultRateLimiterRegistry<R> implements RateLimiterRegistry<R> {

    // TODO - Use shared instances of these. Both classes are used in DefaultMatcherRegistry
    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final MatcherRegistry<R> matcherRegistry;

    private final Map<String, RateLimiterConfigBuilder<?, ?>> configurations;

    private final RateLimiterConfigBuilder<?, ?> defaultConfiguration;

    private RateRecordedListener rootRateRecordedListener;

    private final Map<String, RateLimiterFactory<?>> rateLimiterFactories;

    private RateLimiterFactory<?> defaultRateLimiterFactory;

    public DefaultRateLimiterRegistry(
            MatcherRegistry<R> matcherRegistry,
            RateLimiterConfig<?, ?> rateLimiterConfig,
            RateLimiterFactory<?> rateLimiterFactory,
            @Nullable RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.matcherRegistry = Objects.requireNonNull(matcherRegistry);
        this.configurations = new HashMap<>();
        this.defaultConfiguration = RateLimiterConfig.builder(rateLimiterConfig);
        this.rootRateRecordedListener = RateRecordedListener.NO_OP;
        this.rateLimiterFactories = new HashMap<>();
        this.defaultRateLimiterFactory = Objects.requireNonNull(rateLimiterFactory);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
    }

    @Override public Matcher<R, String> matchAllUris() {
        return matcherRegistry.matchAllUris();
    }

    @Override public Matcher<R, ?> getMatcherOrDefault(String name, Object source) {
        return matcherRegistry.getMatcherOrDefault(name, source);
    }

    @Override public Matcher<R, ?> getOrCreateMatcher(String name, Class<?> clazz) {
        return matcherRegistry.getOrCreateMatcher(name, clazz);
    }

    @Override public Matcher<R, ?> getOrCreateMatcher(String name, Method clazz) {
        return matcherRegistry.getOrCreateMatcher(name, clazz);
    }

    @Override public Matcher<R, ?> getMatcherOrDefault(String name, Matcher<R, ?> resultIfNone) {
        return matcherRegistry.getMatcherOrDefault(name, resultIfNone);
    }

    @Override public RateLimiterRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(clazz, matcher);
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(method, matcher);
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(name, matcher);
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateCache(RateCache<?, ?> rateCache) {
        defaultConfiguration.rateCache(Objects.requireNonNull((RateCache)rateCache));
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache) {
        return registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public RateLimiterRegistry<R> registerRateCache(Method method, RateCache<?, ?> rateCache) {
        return registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public RateLimiterRegistry<R> registerRateCache(String name, RateCache<?, ?> rateCache) {
        getOrCreateConfigurationWithDefaults(name).rateCache((RateCache)Objects.requireNonNull(rateCache));
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateFactory(RateFactory rateFactory) {
        defaultConfiguration.rateFactory(Objects.requireNonNull(rateFactory));
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateFactory(Class<?> clazz, RateFactory rateFactory) {
        return registerRateFactory(classNameProvider.getId(clazz), rateFactory);
    }

    @Override public RateLimiterRegistry<R> registerRateFactory(Method method, RateFactory rateFactory) {
        return registerRateFactory(methodNameProvider.getId(method), rateFactory);
    }

    @Override public RateLimiterRegistry<R> registerRateFactory(String name, RateFactory rateFactory) {
        getOrCreateConfigurationWithDefaults(name).rateFactory(Objects.requireNonNull(rateFactory));
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateRecordedListener(RateRecordedListener rateRecordedListener) {
        defaultConfiguration.rateRecordedListener(Objects.requireNonNull(rateRecordedListener));
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener) {
        return registerRateRecordedListener(classNameProvider.getId(clazz), rateRecordedListener);
    }

    @Override public RateLimiterRegistry<R> registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener) {
        return registerRateRecordedListener(methodNameProvider.getId(method), rateRecordedListener);
    }

    @Override public RateLimiterRegistry<R> registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener) {
        getOrCreateConfigurationWithDefaults(name).rateRecordedListener(Objects.requireNonNull(rateRecordedListener));
        return this;
    }

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public RateLimiterRegistry<R> registerRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        rootRateRecordedListener = Objects.requireNonNull(rateRecordedListener);
        return this;
    }

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public RateLimiterRegistry<R> addRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        Objects.requireNonNull(rateRecordedListener);
        rootRateRecordedListener = rootRateRecordedListener.andThen(rateRecordedListener);
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory) {
        defaultRateLimiterFactory = Objects.requireNonNull(rateLimiterFactory);
        return this;
    }

    @Override public RateLimiterRegistry<R> registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory) {
        return registerRateLimiterFactory(classNameProvider.getId(clazz), rateLimiterFactory);
    }

    @Override public RateLimiterRegistry<R> registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory) {
        return registerRateLimiterFactory(methodNameProvider.getId(method), rateLimiterFactory);
    }

    @Override public RateLimiterRegistry<R> registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory) {
        rateLimiterFactories.put(name, Objects.requireNonNull(rateLimiterFactory));
        return this;
    }

    @Override public RateLimiterFactory<?> getRateLimiterFactory(String name) {
        return rateLimiterFactories.getOrDefault(name, defaultRateLimiterFactory);
    }

    /**
     * @param name The name of the config to return
     * @return A copy of the rate limiter config identified by the name argument
     */
    @Override public RateLimiterConfig<?, ?> getRateLimiterConfig(String name) {
        RateLimiterConfig<?, ?> rateLimiterConfig = configurations.getOrDefault(name, defaultConfiguration).build();
        if(rootRateRecordedListener == RateRecordedListener.NO_OP) {
            return rateLimiterConfig;
        }
        return RateLimiterConfig.builder(rateLimiterConfig)
                .rateRecordedListener(rootRateRecordedListener.andThen(rateLimiterConfig.getRateRecordedListener()))
                .build();
    }

    private RateLimiterConfigBuilder<?, ?> getOrCreateConfigurationWithDefaults(String name) {
        return configurations.computeIfAbsent(name, key -> RateLimiterConfig.builder(defaultConfiguration.build()));
    }
}
