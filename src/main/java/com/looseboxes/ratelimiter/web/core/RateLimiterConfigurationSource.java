package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.InMemoryRateCache;
import com.looseboxes.ratelimiter.cache.RateCache;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RateLimiterConfigurationSource<R> implements RateLimiterConfigurationRegistry<R> {

    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final Map<String, RequestToIdConverter<R>> converters;

    private final RequestToIdConverter<R> defaultRequestToIdConverter;

    private final Map<String, RateLimiterConfiguration<Object>> configurationMap;

    private final RateLimiterConfiguration<Object> defaultRateLimiterConfiguration;

    public RateLimiterConfigurationSource(
            RequestToIdConverter<R> defaultRequestToIdConverter,
            RateCache<Object> rateCache,
            RateSupplier rateSupplier,
            RateRecordedListener rateRecordedListener,
            RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.converters = new HashMap<>();
        this.defaultRequestToIdConverter = Objects.requireNonNull(defaultRequestToIdConverter);
        this.configurationMap = new HashMap<>();
        this.defaultRateLimiterConfiguration = new RateLimiterConfiguration<>()
                .rateCache(rateCache == null ? new InMemoryRateCache<>() : rateCache)
                .rateSupplier(rateSupplier == null ? new LimitWithinDurationSupplier() : rateSupplier)
                .rateExceededHandler(rateRecordedListener == null ? new RateExceededExceptionThrower() : rateRecordedListener);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.addRequestToIdConverters(this);
            rateLimiterConfigurer.addRateCaches(this);
            rateLimiterConfigurer.addRateSuppliers(this);
            rateLimiterConfigurer.addRateExceededHandlers(this);
        }
    }

    @Override public void registerRequestToIdConverter(Class<?> clazz,
            RequestToIdConverter<R> requestToIdConverter) {
        registerRequestToIdConverter(classNameProvider.getId(clazz), requestToIdConverter);
    }

    @Override public void registerRequestToIdConverter(Method method,
            RequestToIdConverter<R> requestToIdConverter) {
        registerRequestToIdConverter(methodNameProvider.getId(method), requestToIdConverter);
    }

    @Override public void registerRequestToIdConverter(String name,
            RequestToIdConverter<R> requestToIdConverter) {
        converters.put(name, requestToIdConverter);
    }

    public RequestToIdConverter<R> getDefaultRequestToIdConverter() {
        return defaultRequestToIdConverter;
    }

    public RequestToIdConverter<R> getRequestToIdConverterOrDefault(String name) {
        return converters.getOrDefault(name, defaultRequestToIdConverter);
    }

    @Override public void registerRateCache(Class<?> clazz, RateCache rateCache) {
        registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public void registerRateCache(Method method, RateCache rateCache) {
        registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public void registerRateCache(String name, RateCache rateCache) {
        getOrCreateConfiguration(name).rateCache(rateCache);
    }

    @Override public void registerRateSupplier(Class<?> clazz, RateSupplier rateSupplier) {
        registerRateSupplier(classNameProvider.getId(clazz), rateSupplier);
    }

    @Override public void registerRateSupplier(Method method, RateSupplier rateSupplier) {
        registerRateSupplier(methodNameProvider.getId(method), rateSupplier);
    }

    @Override public void registerRateSupplier(String name, RateSupplier rateSupplier) {
        getOrCreateConfiguration(name).setRateSupplier(rateSupplier);
    }

    @Override public void registerRateExceededHandler(Class<?> clazz, RateRecordedListener rateRecordedListener) {
        registerRateExceededHandler(classNameProvider.getId(clazz), rateRecordedListener);
    }

    @Override public void registerRateExceededHandler(Method method, RateRecordedListener rateRecordedListener) {
        registerRateExceededHandler(methodNameProvider.getId(method), rateRecordedListener);
    }

    @Override public void registerRateExceededHandler(String name, RateRecordedListener rateRecordedListener) {
        getOrCreateConfiguration(name).setRateExceededHandler(rateRecordedListener);
    }

    public RateLimiterConfiguration<Object> copyConfigurationOrDefault(String name) {
        return new RateLimiterConfiguration<>(configurationMap.getOrDefault(name, defaultRateLimiterConfiguration));
    }

    private RateLimiterConfiguration<Object> getOrCreateConfiguration(String name) {
        return configurationMap.computeIfAbsent(name, (s) -> new RateLimiterConfiguration<>(defaultRateLimiterConfiguration));
    }
}
