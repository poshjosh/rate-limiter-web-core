package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebRateLimiterRegistryTest {

    @Test
    void shouldCreateRateLimiterFactory() {
        WebRateLimiterContext config = WebRateLimiterContext.builder()
                .resourceInfoProvider(ResourceInfoProvider.NONE)
                .classes(this.getClass())
                .build();
        RateLimiterFactory<HttpServletRequest> rateLimiterFactory =
                WebRateLimiterRegistry.of(config).createRateLimiterFactory();
        assertNotNull(rateLimiterFactory);
    }
}