package com.looseboxes.ratelimiter.web.core;

import java.util.Objects;

final class ProxyRegistry<I, O> implements Registry<O> {

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
