package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    Registry<ResourceLimiter<?>> limiters();

    Registry<Matcher<REQUEST, ?>> matchers();

    Registry<RateCache<?>> caches();

    Registry<UsageListener> listeners();
}
