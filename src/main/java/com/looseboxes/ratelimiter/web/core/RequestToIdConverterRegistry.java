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

    public void registerConverter(String rateLimiterName, RequestToIdConverter<R> requestToIdConverter) {
        converters.put(rateLimiterName, requestToIdConverter);
    }

    public RequestToIdConverter<R> getConverterOrDefault(String rateLimiterName) {
        return converters.getOrDefault(rateLimiterName, defaultRequestToIdConverter);
    }

    public RequestToIdConverter<R> getConverter(String rateLimiterName) {
        return converters.get(rateLimiterName);
    }
}
