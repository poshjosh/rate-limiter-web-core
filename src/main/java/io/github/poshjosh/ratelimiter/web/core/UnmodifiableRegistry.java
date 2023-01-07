package io.github.poshjosh.ratelimiter.web.core;

import java.util.Optional;

public interface UnmodifiableRegistry<T> {

    default Optional<T> get(String name) {
        return Optional.ofNullable(getOrDefault(name, null));
    }

    default T getOrDefault(String name) {
        return getOrDefault(name, getDefault());
    }

    T getOrDefault(String name, T resultIfNone);

    T getDefault();
}
