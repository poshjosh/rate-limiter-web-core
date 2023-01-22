package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;

import java.util.Optional;

public interface MatcherFactory<R> {
    Optional<Matcher<R, ?>> createMatcher(String name, RateConfig source);
}
