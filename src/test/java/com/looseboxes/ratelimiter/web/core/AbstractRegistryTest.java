package com.looseboxes.ratelimiter.web.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractRegistryTest<T> {

    protected abstract AccessibleRegistry<T> getInstance();

    protected void shouldRegister(T value) {
        AccessibleRegistry<T> instance = getInstance();
        instance.register(value);
        assertEquals(value, instance.getDefault());
    }

    protected void shouldRegisterByName(T value) {
        AccessibleRegistry<T> instance = getInstance();
        final String name = "name";
        instance.register(name, value);
        assertEquals(instance.getOrDefault(name), value);
    }
}
