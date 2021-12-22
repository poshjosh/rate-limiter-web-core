package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.util.RateLimitConfig;

import java.io.Serializable;

public interface RateLimiterFactory {
    <K extends Serializable, V extends Serializable> RateLimiter<K> createRateLimiter(
            RateLimiterConfiguration<K, V> rateLimiterConfiguration, RateLimitConfig rateLimitConfig);
}
