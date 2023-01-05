package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    Registry<ResourceLimiter<?>> limiters();

    Registry<Matcher<REQUEST, ?>> matchers();

    <K> Registry<RateCache<K>> caches();

    Registry<UsageListener> listeners();
}
