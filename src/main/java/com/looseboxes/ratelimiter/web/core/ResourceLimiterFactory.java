package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateToBandwidthConverter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.ResourceLimiterConfig;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.impl.DefaultResourceLimiterFactory;

public interface ResourceLimiterFactory<K> {

    static <K> ResourceLimiterFactory<K> of() {
        return new DefaultResourceLimiterFactory<>();
    }

    default ResourceLimiter<K> createNew(Rates rates) {
        return createNew(RateToBandwidthConverter.of().convert(rates));
    }

    default ResourceLimiter<K> createNew(Bandwidths rates) {
        return createNew(ResourceLimiterConfig.of(), rates);
    }

    default ResourceLimiter<K> createNew(ResourceLimiterConfig<K, ?> resourceLimiterConfig, Rates rates) {
        return createNew(resourceLimiterConfig, RateToBandwidthConverter.of().convert(rates));
    }

    ResourceLimiter<K> createNew(ResourceLimiterConfig<K, ?> resourceLimiterConfig, Bandwidths rates);
}
