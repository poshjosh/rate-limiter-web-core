package io.github.poshjosh.ratelimiter.web.core.registry;

import java.util.Optional;

public interface Registry<T> {

    static <T> Registry<T> ofDefaults() {
        return new DefaultRegistry<>();
    }

    default Optional<T> get(String name) {
        return Optional.ofNullable(getOrDefault(name, null));
    }

    T getOrDefault(String name, T resultIfNone);

    Registry<T> register(String name, T what);
}
