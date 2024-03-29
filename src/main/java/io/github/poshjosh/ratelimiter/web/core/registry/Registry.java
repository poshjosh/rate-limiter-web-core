package io.github.poshjosh.ratelimiter.web.core.registry;

public interface Registry<T> extends UnmodifiableRegistry<T> {

    static <T> Registry<T> of(T defaultInstance) {
        return new DefaultRegistry<>(defaultInstance);
    }

    static <T> UnmodifiableRegistry<T> unmodifiable(Registry<T> registry) {
        return new UnmodifiableRegistry<T>() {
            @Override public T getOrDefault(String name, T resultIfNone) {
                return registry.getOrDefault(name, resultIfNone);
            }
            @Override public T getDefault() {
                return registry.getDefault();
            }
            @Override public String toString() { return "Unmodifiable{" + registry + "}"; }
        };
    }

    Registry<T> register(T what);

    Registry<T> register(String name, T what);
}
