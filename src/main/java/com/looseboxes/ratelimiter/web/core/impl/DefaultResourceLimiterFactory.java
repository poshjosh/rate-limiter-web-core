package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.ResourceLimiterConfig;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.web.core.ResourceLimiterFactory;

public final class DefaultResourceLimiterFactory<K> implements ResourceLimiterFactory<K> {
    public DefaultResourceLimiterFactory() {}
    @Override
    public ResourceLimiter<K> createNew(ResourceLimiterConfig<K, ?> resourceLimiterConfig, Bandwidths rates) {
        return ResourceLimiter.of(resourceLimiterConfig, rates);
    }
}
