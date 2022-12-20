package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.web.core.AbstractRegistryTest;
import com.looseboxes.ratelimiter.web.core.Registry;
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
        return SimpleRegistry.of("default", IdProvider.forClass(), IdProvider.forMethod());
    }
}
