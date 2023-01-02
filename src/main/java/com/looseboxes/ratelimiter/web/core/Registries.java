package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    Registry<ResourceLimiter<?>> resourceLimiters();

    Registry<Matcher<REQUEST, ?>> matchers();

    Registry<ResourceLimiterFactory<?>> factories();

    Registry<ResourceLimiterConfig<?, ?>> configs();

    <K, V> Registry<RateCache<K, V>> caches();

    Registry<ResourceUsageListener> listeners();
}
