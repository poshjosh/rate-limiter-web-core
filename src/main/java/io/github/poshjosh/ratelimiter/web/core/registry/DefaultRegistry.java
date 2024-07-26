package io.github.poshjosh.ratelimiter.web.core.registry;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class DefaultRegistry<T> implements Registry<T> {

    private final Map<String, T> registered;

    DefaultRegistry() {
        this.registered = new ConcurrentHashMap<>();
    }

    @Override public Registry<T> register(String name, T instance) {
        registered.put(name, Objects.requireNonNull(instance));
        return this;
    }

    @Override public T getOrDefault(String name, T resultIfNone) {
        return registered.getOrDefault(name, resultIfNone);
    }

    @Override public String toString() {
        return "DefaultRegistry{" + "registered=" + registered.keySet() + '}';
    }
}
