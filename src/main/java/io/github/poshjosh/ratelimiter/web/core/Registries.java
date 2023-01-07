package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.util.Matcher;

public interface Registries<REQUEST> {

    static <R> Registries<R> ofDefaults() {
        return of(ResourceLimiter.noop(), Matcher.matchNone(),
                  RateCache.ofMap(), UsageListener.NO_OP);
    }

    static <R> Registries<R> of(
            ResourceLimiter<?> resourceLimiter, Matcher<R, ?> matcher,
            RateCache<?> rateCache, UsageListener usageListener) {
        return new DefaultRegistries<>(resourceLimiter, matcher, rateCache, usageListener);
    }

    Registry<ResourceLimiter<?>> limiters();

    Registry<Matcher<REQUEST, ?>> matchers();

    Registry<RateCache<?>> caches();

    Registry<UsageListener> listeners();
}
