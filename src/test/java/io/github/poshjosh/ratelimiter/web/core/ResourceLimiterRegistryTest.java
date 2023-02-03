package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLimiterRegistryTest {

    @Test void shouldCreateResourceLimiter() {
        PathPatternsProvider pathPatternsProvider = element -> PathPatterns.none();
        ResourceLimiterConfig<HttpServletRequest> config = ResourceLimiterConfig.builderOfRequest()
                .pathPatternsProvider(pathPatternsProvider)
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