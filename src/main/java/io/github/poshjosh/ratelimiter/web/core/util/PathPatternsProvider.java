package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.annotation.RateSource;

public interface PathPatternsProvider {
    PathPatterns<String> get(RateSource source);
}
