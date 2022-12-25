package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsMatcher;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<T, E> implements MatcherFactory<T, E> {

    private final IdProvider<E, PathPatterns<String>> pathPatternsProvider;
    private final RequestToIdConverter<T, String> requestToUriConverter;

    DefaultMatcherFactory(
            IdProvider<E, PathPatterns<String>> pathPatternsProvider,
            RequestToIdConverter<T, String> requestToUriConverter) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public Optional<Matcher<T, ?>> createMatcher(String name, E source) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.getId(source);
        return Optional.of(new PathPatternsMatcher<>(pathPatterns, requestToUriConverter));
    }
}
