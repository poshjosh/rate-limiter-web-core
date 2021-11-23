package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    default void addConverters(RequestToIdConverterRegistry<R> registry) { }
}
