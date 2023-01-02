package com.looseboxes.ratelimiter.web.core;

public interface Registry<T> {

    static <T> Registry<T> of(T defaultInstance) {
        return new SimpleRegistry<>(defaultInstance);
    }

    default T getOrDefault(String name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(String name, T resultIfNone);

    T getDefault();

    Registry<T> register(T what);

    Registry<T> register(String name, T what);
}
