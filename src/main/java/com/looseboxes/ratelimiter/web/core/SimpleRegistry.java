package com.looseboxes.ratelimiter.web.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleRegistry<T> implements Registry<T> {

    public static <T> Registry<T> of(T defaultInstance) {
        return new SimpleRegistry<>(defaultInstance);
    }

    private final Map<String, T> registered;

    private T defaultInstance;

    SimpleRegistry(T defaultInstance) {
        this.registered = new ConcurrentHashMap<>();
        this.defaultInstance = Objects.requireNonNull(defaultInstance);
    }

    @Override public Registry<T> register(T rateLimiterFactory) {
        defaultInstance = Objects.requireNonNull(rateLimiterFactory);
        return this;
    }

    @Override public Registry<T> register(String name, T rateLimiterFactory) {
        registered.put(name, Objects.requireNonNull(rateLimiterFactory));
        return this;
    }

    @Override public T getOrDefault(String name, T resultIfNone) {
        return registered.getOrDefault(name, resultIfNone);
    }

    @Override public T getDefault() {
        return this.defaultInstance;
    }

    Map<String, T> getRegistered() {
        return registered;
    }
}
