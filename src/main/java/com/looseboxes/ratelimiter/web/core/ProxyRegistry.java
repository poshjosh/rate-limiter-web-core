package com.looseboxes.ratelimiter.web.core;

import java.util.Objects;

final class ProxyRegistry<I, O> implements Registry<O> {

    public interface Proxy<I, O>{
        I set(I input, O output);
    }

    private final AccessibleRegistry<I> delegate;

    private final Proxy<I, O> proxy;

    ProxyRegistry(AccessibleRegistry<I> delegate, Proxy<I, O> proxy) {
        this.delegate = Objects.requireNonNull(delegate);
        this.proxy = Objects.requireNonNull(proxy);
    }

    @Override
    public Registry<O> register(O what) {
        I defaultInstance = delegate.getDefault();
        delegate.register(proxy.set(defaultInstance, what));
        return this;
    }

    @Override
    public Registry<O> register(String name, O what) {
        I input = delegate.getOrDefault(name, null);
        return register(input, what);
    }

    private Registry<O> register(I input, O what) {
        Objects.requireNonNull(input);
        delegate.register(proxy.set(input, what));
        return this;
    }
}
