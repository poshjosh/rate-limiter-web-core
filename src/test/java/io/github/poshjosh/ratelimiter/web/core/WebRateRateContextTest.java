package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebRateRateContextTest {

    @Test void build_withoutResourceInfoProvider_shouldThrowException() {
        assertThrows(RuntimeException.class, () -> WebRateLimiterContext.builder().build());
    }

    @Test void build_withResourceInfoProvider_shouldSucceed() {
        WebRateLimiterContext config = WebRateLimiterContext.builder()
                .resourceInfoProvider(ResourceInfoProvider.NONE)
                .classes(this.getClass())
                .build();
        assertNotNull(config);
    }
}