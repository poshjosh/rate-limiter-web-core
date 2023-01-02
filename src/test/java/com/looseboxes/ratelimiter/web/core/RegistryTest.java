package com.looseboxes.ratelimiter.web.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryTest {

    @Test
    void shouldRegister() {
        shouldRegister("value");
    }

    @Test
    void shouldRegisterByName() {
        shouldRegisterByName("value");
    }

    protected void shouldRegister(String value) {
        Registry<String> instance = getInstance();
        instance.register(value);
        assertEquals(value, instance.getDefault());
    }

    protected void shouldRegisterByName(String value) {
        Registry<String> instance = getInstance();
        final String name = "name";
        instance.register(name, value);
        assertEquals(instance.getOrDefault(name), value);
    }

    private Registry<String> getInstance() {
        return Registry.of("default");
    }
}
