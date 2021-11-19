package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    void addConverters(RequestToIdConverterRegistry<R> registry);
}
