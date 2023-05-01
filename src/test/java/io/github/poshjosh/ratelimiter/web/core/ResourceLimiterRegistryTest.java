package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceLimiterRegistryTest {

    @Test void shouldCreateResourceLimiter() {
        ResourceInfoProvider resourceInfoProvider = ResourceInfoProvider.NONE;
        ResourceLimiterConfig config = ResourceLimiterConfig.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build();
        ResourceLimiter<HttpServletRequest> limiter =
                ResourceLimiterRegistry.of(config).createResourceLimiter();
        assertTrue(limiter != null);
    }

    @Test void isRateLimited() {
    }

    @Test void properties() {
    }

    @Test void testIsRateLimited() {
    }

    @Test void testIsRateLimited1() {
    }

    @Test void isRateLimitingEnabled() {
    }

    @Test void matchers() {
    }

    @Test void listeners() {
    }

    @Test void getStore() {
    }

    @Test void getListener() {
    }
}