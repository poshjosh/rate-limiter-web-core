package com.looseboxes.ratelimiter.web.core;

import java.util.HashMap;
import java.util.Map;

public class RequestToIdConverterRegistry<R> {

    private final Map<String, RequestToIdConverter<R>> converters;

    public RequestToIdConverterRegistry(RateLimiterConfigurer<R> rateLimiterConfigurer) {
        converters = new HashMap<>();
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.addConverters(this);
        }
    }

    public void registerConverter(String rateLimiterName, RequestToIdConverter<R> requestToIdConverter) {
        converters.put(rateLimiterName, requestToIdConverter);
    }

    public RequestToIdConverter<R> getConverter(String rateLimiterName) {
        return converters.get(rateLimiterName);
    }
}
