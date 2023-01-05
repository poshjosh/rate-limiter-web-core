package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.util.Matcher;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class DefaultRegistries<R> implements Registries<R> {

    private static final class ProxyRegistry<I, O> implements Registry<O> {

        public interface Proxy<I, O>{
            default O getOrDefault(I input, O resultIfNone) {
                O found = get(input);
                return found == null ? resultIfNone : found;
            }
            O get(I input);
            I set(I input, O output);
        }

        private final Registry<I> delegate;

        private final Proxy<I, O> proxy;

        ProxyRegistry(Registry<I> delegate, Proxy<I, O> proxy) {
            this.delegate = Objects.requireNonNull(delegate);
            this.proxy = Objects.requireNonNull(proxy);
        }

        @Override public Registry<O> register(O what) {
            I defaultInstance = delegate.getDefault();
            delegate.register(proxy.set(defaultInstance, what));
            return this;
        }

        @Override public Registry<O> register(String name, O what) {
            I input = delegate.getOrDefault(name, null);
            return register(input, what);
        }

        private Registry<O> register(I input, O what) {
            Objects.requireNonNull(input);
            delegate.register(proxy.set(input, what));
            return this;
        }

        @Override public O getOrDefault(String name) {
            return proxy.get(delegate.getOrDefault(name));
        }

        @Override public O getDefault() {
            return proxy.get(delegate.getDefault());
        }

        @Override public O getOrDefault(String name, O resultIfNone) {
            I input = delegate.getOrDefault(name, null);
            return getOrDefault(input, resultIfNone);
        }

        private O getOrDefault(I input, O resultIfNone) {
            if (input == null) {
                return resultIfNone;
            }
            return proxy.getOrDefault(input, resultIfNone);
        }
    }

    private final Registry<ResourceLimiter<?>> resourceLimiterRegistry;

    private final Registry<Matcher<R, ?>> matcherRegistry;

    DefaultRegistries(ResourceLimiter<?> resourceLimiter, Matcher<R, ?> matcher) {
        this.resourceLimiterRegistry = Registry.of(new ModifiableResourceLimiter<>(resourceLimiter));
        this.matcherRegistry = Registry.of(matcher);
    }

    @Override
    public Registry<ResourceLimiter<?>> limiters() {
        return resourceLimiterRegistry;
    }

    @Override
    public Registry<Matcher<R, ?>> matchers() {
        return matcherRegistry;
    }

    private static final class CacheProxy<K> implements ProxyRegistry.Proxy<ResourceLimiter<?>, RateCache<K>> {
        @Override public RateCache<K> get(ResourceLimiter<?> input) {
            return ((ModifiableResourceLimiter)input).cache;
        }
        @Override public ResourceLimiter<?> set(ResourceLimiter<?> input, RateCache<K> output) {
            return input.cache(output);
        }
    }

    private static final class UsageListenerProxy implements ProxyRegistry.Proxy<ResourceLimiter<?>, UsageListener> {
        @Override public UsageListener get(ResourceLimiter<?> input) {
            return ((ModifiableResourceLimiter)input).listener;
        }
        @Override public ResourceLimiter<?> set(ResourceLimiter<?> input, UsageListener output) {
            return input.listener(output);
        }
    }

    @Override
    public <K> Registry<RateCache<K>> caches() {
        return new ProxyRegistry<>(resourceLimiterRegistry, new CacheProxy<>());
    }

    @Override
    public Registry<UsageListener> listeners() {
        return new ProxyRegistry<>(resourceLimiterRegistry, new UsageListenerProxy());
    }

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
            this.listener = Objects.requireNonNull(listener);
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
    }
}
