package com.looseboxes.ratelimiter.web.core;

public interface AccessibleRegistry<T> extends Registry<T>{

    default T getOrDefault(String name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(String name, T resultIfNone);

    T getDefault();

    AccessibleRegistry<T> register(T what);

    AccessibleRegistry<T> register(String name, T what);
}
