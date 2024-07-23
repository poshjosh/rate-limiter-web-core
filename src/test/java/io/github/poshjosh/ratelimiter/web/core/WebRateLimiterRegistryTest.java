package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Rate(1)
class WebRateLimiterRegistryTest {

    @Rate(1)
    void testMethod() { }

    private static Method getMethod() {
        try {
            return WebRateLimiterRegistryTest.class.getDeclaredMethod("testMethod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldRegisterMethod() {
        assertTrue(givenRegistry().register(getMethod()).isRegistered(getMethod()));
    }

    @Test
    void shouldRegisterClass() {
        assertTrue(givenRegistry().register(getClass()).isRegistered(getClass()));
    }

    private WebRateLimiterRegistry givenRegistry() {
        WebRateLimiterContext config = WebRateLimiterContext.builder()
                .resourceInfoProvider(ResourceInfoProvider.NONE)
                .classes(this.getClass())
                .build();
        return WebRateLimiterRegistry.of(config);
    }
}