package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    AccessibleRegistry<ResourceLimiter<?>> limiters();

    AccessibleRegistry<Matcher<REQUEST, ?>> matchers();

    AccessibleRegistry<ResourceLimiterFactory<?>> factories();

    <K, V> Registry<RateCache<K, V>> caches();

    Registry<ResourceUsageListener> listeners();
}
