package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    Registry<Matcher<REQUEST, ?>> matchers();

    Registry<RateLimiterFactory<?>> factories();

    Registry<RateLimiterConfig<?, ?>> configs();

    <K, V> Registry<RateCache<K, V>> caches();

    Registry<RateRecordedListener> listeners();
}
