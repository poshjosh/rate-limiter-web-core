package com.looseboxes.ratelimiter.web.core;

import org.junit.jupiter.api.Test;

class SimpleAccessibleRegistryTest extends AbstractRegistryTest<String> {

    @Test
    void shouldRegister() {
        super.shouldRegister("value");
    }

    @Test
    void shouldRegisterByName() {
        super.shouldRegisterByName("value");
    }

    @Override
    protected AccessibleRegistry<String> getInstance() {
        return SimpleAccessibleRegistry.of("default");
    }
}
