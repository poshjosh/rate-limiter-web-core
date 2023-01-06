package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class DefaultRegistries<R> implements Registries<R> {

    private static final class ModifiableResourceLimiter<R> implements ResourceLimiter<R> {

        private final ResourceLimiter<R> delegate;
        private RateCache<?> cache;
        private UsageListener listener;

        private ModifiableResourceLimiter(ResourceLimiter<R> delegate) {
            this(delegate, RateCache.ofMap(), UsageListener.NO_OP);
        }
        private ModifiableResourceLimiter(
                ResourceLimiter<R> delegate,
                RateCache<?> cache,
                UsageListener listener) {
            this.delegate = Objects.requireNonNull(delegate);
            this.cache = Objects.requireNonNull(cache);
            this.listener = java.util.Objects.requireNonNull(listener);
        }

        @Override public boolean tryConsume(R resource, int permits, long timeout, TimeUnit unit) {
            return delegate.tryConsume(resource, permits, timeout, unit);
        }
        @Override public ResourceLimiter<R> cache(RateCache<?> cache) {
            this.cache = Objects.requireNonNull(cache);
            return new ModifiableResourceLimiter<>(delegate.cache(cache), cache, listener);
        }
        @Override public ResourceLimiter<R> listener(UsageListener listener) {
            this.listener = Objects.requireNonNull(listener);
            return new ModifiableResourceLimiter<>(delegate.listener(listener), cache, listener);
        }
        @Override public String toString() {
            return "ModifiableResourceLimiter{" + "delegate=" + delegate + '}';
        }
    }

    private final Registry<ResourceLimiter<?>> resourceLimiterRegistry;

    private final Registry<Matcher<R, ?>> matcherRegistry;

    private final Registry<RateCache<?>> cacheRegistry;

    private final Registry<UsageListener> listenerRegistry;

    DefaultRegistries(ResourceLimiter<?> resourceLimiter, Matcher<R, ?> matcher,
                      RateCache<?> rateCache, UsageListener usageListener) {
        this.resourceLimiterRegistry = Registry.of(new ModifiableResourceLimiter<>(resourceLimiter));
        this.matcherRegistry = Registry.of(matcher);
        this.cacheRegistry = Registry.of(rateCache);
        this.listenerRegistry = Registry.of(usageListener);
    }

    @Override
    public Registry<ResourceLimiter<?>> limiters() {
        return resourceLimiterRegistry;
    }

    @Override
    public Registry<Matcher<R, ?>> matchers() {
        return matcherRegistry;
    }

    @Override
    public Registry<RateCache<?>> caches() {
        return cacheRegistry;
    }

    @Override
    public Registry<UsageListener> listeners() {
        return listenerRegistry;
    }

    public String toString() {
        return "DefaultRegistries{\nmatcherRegistry=" + matcherRegistry +
                "\nlimiterRegistry=" + resourceLimiterRegistry + "}";
    }
}
