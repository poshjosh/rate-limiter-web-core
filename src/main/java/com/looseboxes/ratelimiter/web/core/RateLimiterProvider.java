package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.util.RateLimitConfig;

public interface RateLimiterProvider {
    <K> RateLimiter<K> getRateLimiter(RateLimiterConfiguration<K> rateLimiterConfiguration, RateLimitConfig rateLimitConfig);
}
