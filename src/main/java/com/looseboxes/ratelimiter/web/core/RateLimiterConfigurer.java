package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    default void configure(RateLimiterRegistry<R> registry) { }
}
