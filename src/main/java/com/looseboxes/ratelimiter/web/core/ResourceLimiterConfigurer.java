package com.looseboxes.ratelimiter.web.core;

public interface ResourceLimiterConfigurer<R> {
    default void configure(Registries<R> registry) { }
}
