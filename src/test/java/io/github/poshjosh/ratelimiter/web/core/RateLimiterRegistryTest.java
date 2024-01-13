package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterRegistryTest {

    @Test void shouldCreateRateLimiterFactory() {
        ResourceInfoProvider resourceInfoProvider = ResourceInfoProvider.NONE;
        RateLimiterContext config = RateLimiterContext.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build();
        RateLimiterFactory<HttpServletRequest> rateLimiterFactory =
                RateLimiterRegistry.of(config).createRateLimiterFactory();
        assertTrue(rateLimiterFactory != null);
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