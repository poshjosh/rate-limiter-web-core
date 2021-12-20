package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.SimpleRateLimiter;
import com.looseboxes.ratelimiter.util.RateLimitConfig;

public class DefaultRateLimiterProvider implements RateLimiterProvider{

    @Override
    public <K> RateLimiter<K> getRateLimiter(
            RateLimiterConfiguration<K> rateLimiterConfiguration, RateLimitConfig rateLimitConfig) {

        return new SimpleRateLimiter<>(rateLimiterConfiguration, rateLimitConfig);
    }
}