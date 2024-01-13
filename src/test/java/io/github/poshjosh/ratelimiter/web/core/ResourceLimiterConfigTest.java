package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLimiterConfigTest {

    @Test void build_withoutResourceInfoProvider_shouldThrowException() {
        assertThrows(RuntimeException.class, () -> ResourceLimiterConfig.builder().build());
    }

    @Test void build_withResourceInfoProvider_shouldSucceed() {
        ResourceLimiterConfig config = ResourceLimiterConfig.builder()
                .resourceInfoProvider(ResourceInfoProvider.NONE)
                .build();
        assertTrue(config != null);
    }
}