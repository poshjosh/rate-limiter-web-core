package com.looseboxes.ratelimiter.web.core;

import org.junit.jupiter.api.Test;

class SimpleRegistryTest extends AbstractRegistryTest<String> {

    @Test
    void shouldRegister() {
        super.shouldRegister("value");
    }

    @Test
    void shouldRegisterByName() {
        super.shouldRegisterByName("value");
    }

    @Override
    protected Registry<String> getInstance() {
        return SimpleRegistry.of("default");
    }
}
