package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;

public interface Registries<REQUEST> {

    MatcherRegistry<REQUEST> matchers();

    Registry<RateLimiterFactory<?>> factories();

    Registry<RateLimiterConfig<?, ?>> configs();

    <K, V> Registry<RateCache<K, V>> caches();

    Registry<RateRecordedListener> listeners();
}
