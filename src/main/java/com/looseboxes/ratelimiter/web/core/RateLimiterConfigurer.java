package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    default void addRequestToIdConverters(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateCaches(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateSuppliers(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateExceededHandlers(RateLimiterConfigurationRegistry<R> registry) { }
}
