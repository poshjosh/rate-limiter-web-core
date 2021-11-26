package com.looseboxes.ratelimiter.web.core;

public interface RateLimiterConfigurer<R> {
    default void addRequestToIdConverters(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateCaches(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateFactorys(RateLimiterConfigurationRegistry<R> registry) { }
    default void addRateRecordedListener(RateLimiterConfigurationRegistry<R> registry) { }
}
