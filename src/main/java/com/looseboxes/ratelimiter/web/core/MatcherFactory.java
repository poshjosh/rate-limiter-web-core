package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.Matcher;

import java.util.Optional;

public interface MatcherFactory<T, E> {
    Optional<Matcher<T, ?>> createMatcher(String name, E source);
}
