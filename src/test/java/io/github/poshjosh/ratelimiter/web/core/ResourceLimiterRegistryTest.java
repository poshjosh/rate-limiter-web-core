package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

class ResourceLimiterRegistryTest {

    @Test void shouldCreateResourceLimiter() {
        ResourceInfoProvider resourceInfoProvider = element -> ResourceInfoProvider.NONE;
        ResourceLimiterConfig config = ResourceLimiterConfig.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build();
        ResourceLimiter<HttpServletRequest> limiter =
                ResourceLimiterRegistry.of(config).createResourceLimiter();
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

    @Test void getStore() {
    }

    @Test void getListener() {
    }
}