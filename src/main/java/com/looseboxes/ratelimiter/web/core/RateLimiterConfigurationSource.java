package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;
import java.util.*;

public class RateLimiterConfigurationSource<R> implements RateLimiterConfigurationRegistry<R> {

    // TODO - Use shared instances of these. Both classes are used in DefaultMatcherRegistry
    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final MatcherRegistry<R> matcherRegistry;

    private final Map<String, RateLimiterConfig<?, ?>> configurations;

    private final RateLimiterConfig<?, ?> defaultConfiguration;

    private RateRecordedListener rootRateRecordedListener;

    private final Map<String, RateLimiterFactory<?>> rateLimiterFactories;

    private RateLimiterFactory<?> defaultRateLimiterFactory;

    public RateLimiterConfigurationSource(
            MatcherRegistry<R> matcherRegistry,
            RateLimiterConfig<?, ?> rateLimiterConfig,
            RateLimiterFactory<?> rateLimiterFactory,
            @Nullable RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.matcherRegistry = Objects.requireNonNull(matcherRegistry);
        this.configurations = new HashMap<>();
        this.defaultConfiguration = Objects.requireNonNull(rateLimiterConfig);
        this.rootRateRecordedListener = RateRecordedListener.NO_OP;
        this.rateLimiterFactories = new HashMap<>();
        this.defaultRateLimiterFactory = Objects.requireNonNull(rateLimiterFactory);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcherForProperties(String name) {
        return matcherRegistry.getOrCreateMatcherForProperties(name);
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcherForSourceElement(String name, Object source) {
        return matcherRegistry.getOrCreateMatcherForSourceElement(name, source);
    }

    @Override public RateLimiterConfigurationSource<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(clazz, matcher);
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(method, matcher);
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRequestMatcher(String name, Matcher<R, ?> matcher) {
        matcherRegistry.registerRequestMatcher(name, matcher);
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateCache(RateCache<?, ?> rateCache) {
        defaultConfiguration.rateCache(Objects.requireNonNull((RateCache)rateCache));
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache) {
        return registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateCache(Method method, RateCache<?, ?> rateCache) {
        return registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateCache(String name, RateCache<?, ?> rateCache) {
        getOrCreateConfigurationWithDefaults(name).rateCache((RateCache)Objects.requireNonNull(rateCache));
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateFactory(RateFactory rateFactory) {
        defaultConfiguration.rateFactory(Objects.requireNonNull(rateFactory));
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateFactory(Class<?> clazz, RateFactory rateFactory) {
        return registerRateFactory(classNameProvider.getId(clazz), rateFactory);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateFactory(Method method, RateFactory rateFactory) {
        return registerRateFactory(methodNameProvider.getId(method), rateFactory);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateFactory(String name, RateFactory rateFactory) {
        getOrCreateConfigurationWithDefaults(name).setRateFactory(Objects.requireNonNull(rateFactory));
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateRecordedListener(RateRecordedListener rateRecordedListener) {
        defaultConfiguration.rateRecordedListener(Objects.requireNonNull(rateRecordedListener));
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener) {
        return registerRateRecordedListener(classNameProvider.getId(clazz), rateRecordedListener);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener) {
        return registerRateRecordedListener(methodNameProvider.getId(method), rateRecordedListener);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener) {
        getOrCreateConfigurationWithDefaults(name).setRateRecordedListener(Objects.requireNonNull(rateRecordedListener));
        return this;
    }

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public RateLimiterConfigurationSource<R> registerRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        rootRateRecordedListener = Objects.requireNonNull(rateRecordedListener);
        return this;
    }

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public RateLimiterConfigurationSource<R> addRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        Objects.requireNonNull(rateRecordedListener);
        rootRateRecordedListener = rootRateRecordedListener.andThen(rateRecordedListener);
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory) {
        defaultRateLimiterFactory = Objects.requireNonNull(rateLimiterFactory);
        return this;
    }

    @Override public RateLimiterConfigurationSource<R> registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory) {
        return registerRateLimiterFactory(classNameProvider.getId(clazz), rateLimiterFactory);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory) {
        return registerRateLimiterFactory(methodNameProvider.getId(method), rateLimiterFactory);
    }

    @Override public RateLimiterConfigurationSource<R> registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory) {
        rateLimiterFactories.put(name, Objects.requireNonNull(rateLimiterFactory));
        return this;
    }

    public RateLimiterFactory<?> getRateLimiterFactory(String name) {
        return rateLimiterFactories.getOrDefault(name, defaultRateLimiterFactory);
    }

    public RateLimiterConfig<?, ?> copyConfigurationOrDefault(String name) {
        RateLimiterConfig<?, ?> result = new RateLimiterConfig<>(configurations.getOrDefault(name, defaultConfiguration));
        if(rootRateRecordedListener != RateRecordedListener.NO_OP) {
            result.setRateRecordedListener(rootRateRecordedListener.andThen(result.getRateRecordedListener()));
        }
        return result;
    }

    private RateLimiterConfig<?, ?> getOrCreateConfigurationWithDefaults(String name) {
        return configurations.computeIfAbsent(name, key -> new RateLimiterConfig<>(defaultConfiguration));
    }
}
