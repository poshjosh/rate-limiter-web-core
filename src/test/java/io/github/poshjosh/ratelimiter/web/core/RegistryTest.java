package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.web.core.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
        final String name = UUID.randomUUID().toString();
        instance.register(name, value);
        assertEquals(value, instance.getOrDefault(name, null));
    }

    protected void shouldRegisterByName(String value) {
        Registry<String> instance = getInstance();
        final String name = "name";
        instance.register(name, value);
        assertEquals(instance.getOrDefault(name, null), value);
    }

    private Registry<String> getInstance() {
        return Registry.ofDefaults();
    }
}
