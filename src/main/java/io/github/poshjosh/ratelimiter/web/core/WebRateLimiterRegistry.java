package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterRegistry;

import javax.servlet.http.HttpServletRequest;

public interface WebRateLimiterRegistry extends RateLimiterRegistry<HttpServletRequest> {

}
