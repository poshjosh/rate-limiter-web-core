package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.util.Matcher;

import java.util.Optional;

public interface MatcherFactory<T> {
    Optional<Matcher<T, ?>> createMatcher(String name, RateConfig source);
}
