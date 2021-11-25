package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.InMemoryRateCache;
import com.looseboxes.ratelimiter.cache.RateCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RateLimiterConfigurationRegistry<R> {

    private final Map<String, RequestToIdConverter<R>> converters;

    private final RequestToIdConverter<R> defaultRequestToIdConverter;

    private final Map<String, RateLimiterConfiguration<Object>> configurationMap;

    private final RateLimiterConfiguration<Object> defaultRateLimiterConfiguration;

    public RateLimiterConfigurationRegistry(
            RequestToIdConverter<R> defaultRequestToIdConverter,
            RateCache<Object> rateCache,
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.converters = new HashMap<>();
        this.defaultRequestToIdConverter = Objects.requireNonNull(defaultRequestToIdConverter);
        this.configurationMap = new HashMap<>();
        this.defaultRateLimiterConfiguration = new RateLimiterConfiguration<>()
                .rateCache(rateCache == null ? new InMemoryRateCache<>() : rateCache)
                .rateSupplier(rateSupplier == null ? new LimitWithinDurationSupplier() : rateSupplier)
                .rateExceededHandler(rateExceededHandler == null ? new RateExceededExceptionThrower() : rateExceededHandler);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
    }

    public void registerConverter(String name, RequestToIdConverter<R> requestToIdConverter) {
        converters.put(name, requestToIdConverter);
    }

    public RequestToIdConverter<R> getDefaultRequestToIdConverter() {
        return defaultRequestToIdConverter;
    }

    public RequestToIdConverter<R> getRequestToIdConverterOrDefault(String name) {
        return converters.getOrDefault(name, defaultRequestToIdConverter);
    }

    public void registerRateCache(String name, RateCache rateCache) {
        getOrCreateConfiguration(name).rateCache(rateCache);
    }

    public void registerRateSupplier(String name, RateSupplier rateSupplier) {
        getOrCreateConfiguration(name).setRateSupplier(rateSupplier);
    }

    public void registerRateExceededHandler(String name, RateExceededHandler rateExceededHandler) {
        getOrCreateConfiguration(name).setRateExceededHandler(rateExceededHandler);
    }

    public RateLimiterConfiguration<Object> copyConfigurationOrDefault(String name) {
        return new RateLimiterConfiguration<>(configurationMap.getOrDefault(name, defaultRateLimiterConfiguration));
    }

    private RateLimiterConfiguration<Object> getOrCreateConfiguration(String name) {
        return configurationMap.computeIfAbsent(name, (s) -> new RateLimiterConfiguration<>(defaultRateLimiterConfiguration));
    }
}
