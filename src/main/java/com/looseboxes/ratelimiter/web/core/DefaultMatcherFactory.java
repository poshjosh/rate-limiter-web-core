package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsMatcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<T> implements MatcherFactory<T, Element> {

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestToIdConverter<T, String> requestToUriConverter;

    DefaultMatcherFactory(
            PathPatternsProvider pathPatternsProvider,
            RequestToIdConverter<T, String> requestToUriConverter) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public Optional<Matcher<T, ?>> createMatcher(String name, Element source) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(source);
        return Optional.of(new PathPatternsMatcher<>(pathPatterns, requestToUriConverter));
    }
}
