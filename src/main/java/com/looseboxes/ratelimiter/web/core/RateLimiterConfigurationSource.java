package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.InMemoryRateCache;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.ElementPatternsMatcher;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RequestUriMatcher;

import java.lang.reflect.Method;
import java.util.*;

public class RateLimiterConfigurationSource<R> implements RateLimiterConfigurationRegistry<R> {

    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final Map<String, RateLimiterProvider> providers;

    private final RateLimiterProvider defaultProvider;

    private final RequestToIdConverter<R, String> requestToUriConverter;

    private final Map<String, Matcher<R>> matchers;

    private final Map<String, RateLimiterConfiguration<Object>> configurations;

    private final RateLimiterConfiguration<Object> defaultConfiguration;

    private RateExceededListener rootRateExceededListener;

    private final IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;

    private final IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;

    private final Matcher<R> matcherForAllRequestUris;

    public RateLimiterConfigurationSource(
            RequestToIdConverter<R, String> requestToUriConverter,
            RateCache<Object> rateCache,
            RateFactory rateFactory,
            RateExceededListener rateExceededListener,
            RateLimiterProvider defaultRateLimiterProvider,
            RateLimiterConfigurer<R> rateLimiterConfigurer,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        this.providers = new HashMap<>();
        this.defaultProvider = Objects.requireNonNull(defaultRateLimiterProvider);
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        this.matchers = new HashMap<>();
        this.configurations = new HashMap<>();
        this.defaultConfiguration = new RateLimiterConfiguration<>()
                .rateCache(rateCache == null ? new InMemoryRateCache<>() : rateCache)
                .rateFactory(rateFactory == null ? new LimitWithinDurationFactory() : rateFactory)
                .rateExceededListener(rateExceededListener == null ? new RateExceededExceptionThrower() :
                        rateExceededListener);
        this.rootRateExceededListener = RateExceededListener.NO_OP;
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
        this.classPathPatternsProvider = Objects.requireNonNull(classPathPatternsProvider);
        this.methodPathPatternsProvider = Objects.requireNonNull(methodPathPatternsProvider);
        this.matcherForAllRequestUris = new RequestUriMatcher<>(requestToUriConverter);
    }

    @Override public void registerRateLimiterProvider(Class<?> clazz, RateLimiterProvider matcher) {
        registerRateLimiterProvider(classNameProvider.getId(clazz), matcher);
    }

    @Override public void registerRateLimiterProvider(Method method, RateLimiterProvider matcher) {
        registerRateLimiterProvider(methodNameProvider.getId(method), matcher);
    }

    @Override public void registerRateLimiterProvider(String name, RateLimiterProvider matcher) {
        providers.put(name, Objects.requireNonNull(matcher));
    }

    public RateLimiterProvider getRateLimiterProvider(String name) {
        return providers.computeIfAbsent(name, key -> defaultProvider);
    }

    @Override public void registerRequestMatcher(Class<?> clazz, Matcher<R> matcher) {
        registerRequestMatcher(classNameProvider.getId(clazz), matcher);
    }

    @Override public void registerRequestMatcher(Method method, Matcher<R> matcher) {
        registerRequestMatcher(methodNameProvider.getId(method), matcher);
    }

    @Override public void registerRequestMatcher(String name, Matcher<R> matcher) {
        matchers.put(name, Objects.requireNonNull(matcher));
    }

    public Matcher<R> getMatcherForProperties(String name) {
        return matchers.computeIfAbsent(name, s -> matcherForAllRequestUris);
    }

    public Matcher<R> getMatcherForSourceElement(String name, Object source) {
        return matchers.computeIfAbsent(name, s -> createMatcherForSourceElement(source));
    }

    private Matcher<R> createMatcherForSourceElement(Object source) {
        if(source instanceof Class) {
            return new ElementPatternsMatcher<>((Class<?>)source, classPathPatternsProvider, requestToUriConverter);
        }else if(source instanceof Method) {
            return  new ElementPatternsMatcher<>((Method)source, methodPathPatternsProvider, requestToUriConverter);
        }else{
            throw new UnsupportedOperationException();
        }
    }

    @Override public void registerRateCache(RateCache<Object> rateCache) {
        defaultConfiguration.rateCache(Objects.requireNonNull(rateCache));
    }

    @Override public void registerRateCache(Class<?> clazz, RateCache<Object> rateCache) {
        registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public void registerRateCache(Method method, RateCache<Object> rateCache) {
        registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public void registerRateCache(String name, RateCache<Object> rateCache) {
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

    @Override public void registerRateExceededListener(RateExceededListener rateExceededListener) {
        defaultConfiguration.rateExceededListener(Objects.requireNonNull(rateExceededListener));
    }

    @Override public void registerRateExceededListener(Class<?> clazz, RateExceededListener rateExceededListener) {
        registerRateExceededListener(classNameProvider.getId(clazz), rateExceededListener);
    }

    @Override public void registerRateExceededListener(Method method, RateExceededListener rateExceededListener) {
        registerRateExceededListener(methodNameProvider.getId(method), rateExceededListener);
    }

    @Override public void registerRateExceededListener(String name, RateExceededListener rateExceededListener) {
        getOrCreateConfigurationWithDefaults(name).setRateExceededListener(Objects.requireNonNull(
                rateExceededListener));
    }

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateExceededListener The listener to register
     */
    @Override public void registerRootRateExceededListener(
            RateExceededListener rateExceededListener) {
        rootRateExceededListener = Objects.requireNonNull(rateExceededListener);
    }

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateExceededListener The listener to register
     */
    @Override public void addRootRateExceededListener(RateExceededListener rateExceededListener) {
        Objects.requireNonNull(rateExceededListener);
        rootRateExceededListener = rootRateExceededListener.andThen(rateExceededListener);
    }

    public RateLimiterConfiguration<Object> copyConfigurationOrDefault(String name) {
        RateLimiterConfiguration<Object> result = new RateLimiterConfiguration<>(
                configurations.getOrDefault(name, defaultConfiguration));
        if(rootRateExceededListener != RateExceededListener.NO_OP) {
            result.setRateExceededListener(rootRateExceededListener.andThen(result.getRateExceededListener()));
        }
        return result;
    }

    private RateLimiterConfiguration<Object> getOrCreateConfigurationWithDefaults(String name) {
        return configurations.computeIfAbsent(name, (s) -> new RateLimiterConfiguration<>(defaultConfiguration));
    }
}
