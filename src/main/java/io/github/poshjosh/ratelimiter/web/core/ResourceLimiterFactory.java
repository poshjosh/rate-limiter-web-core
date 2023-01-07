package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.util.Rates;

public interface ResourceLimiterFactory<K> {

    static <K> ResourceLimiterFactory<K> ofDefaults() {
        return ResourceLimiter::of;
    }

    default ResourceLimiter<K> createNew(Rates rates) {
        return createNew(RateToBandwidthConverter.ofDefaults().convert(rates));
    }

    default ResourceLimiter<K> createNew(RateCache<K> cache, UsageListener listener, Rates rates) {
        return createNew(cache, listener, RateToBandwidthConverter.ofDefaults().convert(rates));
    }

    default ResourceLimiter<K> createNew(Bandwidths rates) {
        return createNew(RateCache.ofMap(), UsageListener.NO_OP, rates);
    }

    ResourceLimiter<K> createNew(RateCache<K> cache, UsageListener listener, Bandwidths rates);
}
