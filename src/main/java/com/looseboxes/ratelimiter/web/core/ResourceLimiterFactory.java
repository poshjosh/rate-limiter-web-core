package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateToBandwidthConverter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.util.Rates;

public interface ResourceLimiterFactory<K> {

    static <K> ResourceLimiterFactory<K> ofDefaults() {
        return ResourceLimiter::of;
    }

    default ResourceLimiter<K> createNew(Rates rates) {
        return createNew(RateToBandwidthConverter.ofDefaults().convert(rates));
    }

    ResourceLimiter<K> createNew(Bandwidths rates);
}
