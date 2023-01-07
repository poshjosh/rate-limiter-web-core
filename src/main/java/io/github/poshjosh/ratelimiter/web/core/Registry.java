package io.github.poshjosh.ratelimiter.web.core;

import java.util.Optional;

public interface Registry<T> {

    static <T> Registry<T> of(T defaultInstance) {
        return new SimpleRegistry<>(defaultInstance);
    }

    default Optional<T> get(String name) {
        return Optional.ofNullable(getOrDefault(name, null));
    }

    default T getOrDefault(String name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(String name, T resultIfNone);

    T getDefault();

    Registry<T> register(T what);

    Registry<T> register(String name, T what);
}
