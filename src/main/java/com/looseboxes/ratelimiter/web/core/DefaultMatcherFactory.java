package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsMatcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<T> implements MatcherFactory<T> {

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestToIdConverter<T, String> requestToUriConverter;

    DefaultMatcherFactory(
            PathPatternsProvider pathPatternsProvider,
            RequestToIdConverter<T, String> requestToUriConverter) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public Optional<Matcher<T, ?>> createMatcher(String name, RateConfig rateConfig) {
        final Object source = rateConfig.getSource();
        if (source instanceof Element) {
            return Optional.of(createElementMatcher((Element)source));
        } else {
            return Optional.empty();
        }
    }

    private Matcher<T, ?> createElementMatcher(Element element) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(element);
        return new PathPatternsMatcher<>(pathPatterns, requestToUriConverter);
    }
}
