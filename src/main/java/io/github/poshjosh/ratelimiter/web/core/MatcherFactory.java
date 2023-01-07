package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;

import java.util.Optional;

public interface MatcherFactory<T> {
    Optional<Matcher<T, ?>> createMatcher(String name, RateConfig source);
}
