package com.looseboxes.ratelimiter.web.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RequestToIdConverterRegistry<R> {

    private final Map<String, RequestToIdConverter<R>> converters;

    private final RequestToIdConverter<R> defaultRequestToIdConverter;

    public RequestToIdConverterRegistry(
            RequestToIdConverter<R> defaultRequestToIdConverter,
            RateLimiterConfigurer<R> rateLimiterConfigurer) {
        this.converters = new HashMap<>();
        this.defaultRequestToIdConverter = Objects.requireNonNull(defaultRequestToIdConverter);
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.addConverters(this);
        }
    }

    public void registerConverter(String name, RequestToIdConverter<R> requestToIdConverter) {
        converters.put(name, requestToIdConverter);
    }

    public RequestToIdConverter<R> getConverterOrDefault(String name) {
        return converters.getOrDefault(name, defaultRequestToIdConverter);
    }

    public RequestToIdConverter<R> getConverter(String name) {
        return converters.get(name);
    }
}
