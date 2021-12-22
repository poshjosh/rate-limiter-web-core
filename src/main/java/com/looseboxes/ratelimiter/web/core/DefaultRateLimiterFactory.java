package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.SimpleRateLimiter;
import com.looseboxes.ratelimiter.util.RateLimitConfig;

import java.io.Serializable;

public class DefaultRateLimiterFactory implements RateLimiterFactory {

    @Override
    public <K extends Serializable, V extends Serializable> RateLimiter<K> createRateLimiter(
            RateLimiterConfiguration<K, V> rateLimiterConfiguration, RateLimitConfig rateLimitConfig) {
        return new SimpleRateLimiter<>(
                rateLimiterConfiguration.getRateCache(),
                rateLimiterConfiguration.getRateFactory(),
                rateLimiterConfiguration.getRateExceededListener(),
                rateLimitConfig);
    }
}
