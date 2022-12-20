package com.looseboxes.ratelimiter.web.core;

import java.lang.reflect.Method;

public interface Registry<T> {

    default T getOrDefault(Class<?> name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(Class<?> name, T resultIfNone);

    default T getOrDefault(Method name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(Method name, T resultIfNone);

    default T getOrDefault(String name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(String name, T resultIfNone);

    T getDefault();

    Registry<T> register(T what);

    Registry<T> register(Class<?> name, T what);

    Registry<T> register(Method name, T what);

    Registry<T> register(String name, T what);
}
