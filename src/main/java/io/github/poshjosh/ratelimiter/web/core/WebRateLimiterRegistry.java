package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterRegistry;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;

public interface WebRateLimiterRegistry extends RateLimiterRegistry<HttpServletRequest> {

    static WebRateLimiterRegistry ofDefaults() {
        return of(rateSource -> ResourceInfoProvider.ResourceInfo.of(PathPatterns.matchALL()));
    }

    static WebRateLimiterRegistry of(ResourceInfoProvider resourceInfoProvider) {
        return of(WebRateLimiterContext.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build());
    }

    static WebRateLimiterRegistry of(WebRateLimiterContext webRateLimiterContext) {
        return new DefaultWebRateLimiterRegistry(webRateLimiterContext);
    }

    boolean hasMatching(String id);

    UnmodifiableRegistries registries();
}
