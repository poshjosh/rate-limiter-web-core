package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.ElementPatternsMatcher;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RequestUriMatcher;

import java.lang.reflect.Method;
import java.util.*;

public class RateLimiterConfigurationSource<R> implements RateLimiterConfigurationRegistry<R> {

    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final Map<String, RateLimiterConfig<?, ?>> configurations;

    private final RateLimiterConfig defaultConfiguration;

    private final RequestToIdConverter<R, String> requestToUriConverter;

    private final Map<String, Matcher<R, ?>> matchers;

    private final Matcher<R, ?> matcherForAllRequestUris;

    private RateRecordedListener rootRateRecordedListener;

    private final Map<String, RateLimiterFactory<?>> rateLimiterFactories;

    private RateLimiterFactory<?> defaultRateLimiterFactory;

    private final IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;

    private final IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;

    public RateLimiterConfigurationSource(
            RequestToIdConverter<R, String> requestToUriConverter,
            RateCache rateCache,
            RateFactory rateFactory,
            RateRecordedListener rateRecordedListener,
            RateLimiterFactory<?> defaultRateLimiterFactory,
            RateLimiterConfigurer<R> rateLimiterConfigurer,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        this.matchers = new HashMap<>();
        this.matcherForAllRequestUris = new RequestUriMatcher<>(requestToUriConverter);
        this.configurations = new HashMap<>();
        defaultConfiguration = new RateLimiterConfig<>();
        if (rateCache != null) {
            defaultConfiguration.rateCache(rateCache);
        }
        if(rateFactory != null) {
            defaultConfiguration.rateFactory(rateFactory);
        }
        if(rateRecordedListener != null) {
            defaultConfiguration.rateExceededListener(rateRecordedListener);
        }
        this.rootRateRecordedListener = RateRecordedListener.NO_OP;
        this.rateLimiterFactories = new HashMap<>();
        this.defaultRateLimiterFactory = Objects.requireNonNull(defaultRateLimiterFactory);
        this.classPathPatternsProvider = Objects.requireNonNull(classPathPatternsProvider);
        this.methodPathPatternsProvider = Objects.requireNonNull(methodPathPatternsProvider);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
    }

    @Override public void registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher) {
        registerRequestMatcher(classNameProvider.getId(clazz), matcher);
    }

    @Override public void registerRequestMatcher(Method method, Matcher<R, ?> matcher) {
        registerRequestMatcher(methodNameProvider.getId(method), matcher);
    }

    @Override public void registerRequestMatcher(String name, Matcher<R, ?> matcher) {
        matchers.put(name, Objects.requireNonNull(matcher));
    }

    public Matcher<R, ?> getMatcherForProperties(String name) {
        return matchers.getOrDefault(name, matcherForAllRequestUris);
    }

    public Matcher<R, ?> getMatcherForSourceElement(String name, Object source) {
        return matchers.computeIfAbsent(name, key -> createMatcherForSourceElement(source));
    }

    private Matcher<R, ?> createMatcherForSourceElement(Object source) {
        if(source instanceof Class) {
            return new ElementPatternsMatcher<>((Class<?>)source, classPathPatternsProvider, requestToUriConverter);
        }else if(source instanceof Method) {
            return  new ElementPatternsMatcher<>((Method)source, methodPathPatternsProvider, requestToUriConverter);
        }else{
            throw new UnsupportedOperationException();
        }
    }

    @Override public void registerRateCache(RateCache<?, ?> rateCache) {
        defaultConfiguration.rateCache(Objects.requireNonNull(rateCache));
    }

    @Override public void registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache) {
        registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public void registerRateCache(Method method, RateCache<?, ?> rateCache) {
        registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public void registerRateCache(String name, RateCache<?, ?> rateCache) {
        getOrCreateConfigurationWithDefaults(name).rateCache(Objects.requireNonNull(rateCache));
    }

    @Override public void registerRateFactory(RateFactory rateFactory) {
        defaultConfiguration.rateFactory(Objects.requireNonNull(rateFactory));
    }

    @Override public void registerRateFactory(Class<?> clazz, RateFactory rateFactory) {
        registerRateFactory(classNameProvider.getId(clazz), rateFactory);
    }

    @Override public void registerRateFactory(Method method, RateFactory rateFactory) {
        registerRateFactory(methodNameProvider.getId(method), rateFactory);
    }

    @Override public void registerRateFactory(String name, RateFactory rateFactory) {
        getOrCreateConfigurationWithDefaults(name).setRateFactory(Objects.requireNonNull(rateFactory));
    }

    @Override public void registerRateExceededListener(RateRecordedListener rateRecordedListener) {
        defaultConfiguration.rateExceededListener(Objects.requireNonNull(rateRecordedListener));
    }

    @Override public void registerRateExceededListener(Class<?> clazz, RateRecordedListener rateRecordedListener) {
        registerRateExceededListener(classNameProvider.getId(clazz), rateRecordedListener);
    }

    @Override public void registerRateExceededListener(Method method, RateRecordedListener rateRecordedListener) {
        registerRateExceededListener(methodNameProvider.getId(method), rateRecordedListener);
    }

    @Override public void registerRateExceededListener(String name, RateRecordedListener rateRecordedListener) {
        getOrCreateConfigurationWithDefaults(name).setRateExceededListener(Objects.requireNonNull(rateRecordedListener));
    }

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public void registerRootRateExceededListener(RateRecordedListener rateRecordedListener) {
        rootRateRecordedListener = Objects.requireNonNull(rateRecordedListener);
    }

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public void addRootRateExceededListener(RateRecordedListener rateRecordedListener) {
        Objects.requireNonNull(rateRecordedListener);
        rootRateRecordedListener = rootRateRecordedListener.andThen(rateRecordedListener);
    }

    @Override public void registerDefaultRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory) {
        defaultRateLimiterFactory = Objects.requireNonNull(rateLimiterFactory);
    }

    @Override public void registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory) {
        registerRateLimiterFactory(classNameProvider.getId(clazz), rateLimiterFactory);
    }

    @Override public void registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory) {
        registerRateLimiterFactory(methodNameProvider.getId(method), rateLimiterFactory);
    }

    @Override public void registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory) {
        rateLimiterFactories.put(name, Objects.requireNonNull(rateLimiterFactory));
    }

    public RateLimiterFactory<?> getRateLimiterFactory(String name) {
        return rateLimiterFactories.getOrDefault(name, defaultRateLimiterFactory);
    }

    public RateLimiter<?> createRateLimiter(String name, RateConfigList rateConfigList) {
        RateLimiterConfig rateLimiterConfig = copyConfigurationOrDefault(name);
        return getRateLimiterFactory(name).createRateLimiter(rateLimiterConfig,
                rateConfigList);
    }

    public RateLimiterConfig<?, ?> copyConfigurationOrDefault(String name) {
        RateLimiterConfig<?, ?> result = new RateLimiterConfig<>(
                configurations.getOrDefault(name, defaultConfiguration));
        if(rootRateRecordedListener != RateRecordedListener.NO_OP) {
            result.setRateExceededListener(rootRateRecordedListener.andThen(result.getRateExceededListener()));
        }
        return result;
    }

    private RateLimiterConfig getOrCreateConfigurationWithDefaults(String name) {
        return configurations.computeIfAbsent(name, key -> new RateLimiterConfig<>(defaultConfiguration));
    }
}
