package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

public interface WebRateLimiterRegistries {
    static WebRateLimiterRegistry of(ResourceInfoProvider resourceInfoProvider) {
        return of(WebRateLimiterContext.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build());
    }

    static WebRateLimiterRegistry of(WebRateLimiterContext webRateLimiterContext) {
        return new DefaultWebRateLimiterRegistry(webRateLimiterContext);
    }
}
