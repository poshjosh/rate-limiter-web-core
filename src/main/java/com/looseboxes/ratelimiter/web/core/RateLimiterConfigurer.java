package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    default void configure(RateLimiterConfigurationRegistry<R> registry) { }
}
