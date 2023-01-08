package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsMatcher;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RequestRates;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<T> implements MatcherFactory<T> {

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestMatcherFactory<T> requestMatcherFactory;

    DefaultMatcherFactory(
            PathPatternsProvider pathPatternsProvider,
            RequestMatcherFactory<T> requestMatcherFactory) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestMatcherFactory = Objects.requireNonNull(requestMatcherFactory);
    }

    @Override
    public Optional<Matcher<T, ?>> createMatcher(String name, RateConfig rateConfig) {
        final Object source = rateConfig.getSource();
        final Rates rates = rateConfig.getValue();
        if (source instanceof Element) {
            Matcher main = createElementMatcher((Element)source);
            Optional<Matcher<T, ?>> supplOptional = createSupplementaryMatcher(rates);
            if (!supplOptional.isPresent()) {
                return Optional.of(main);
            }
            return Optional.of(main.andThen(supplOptional.get()));
        }
        return Optional.empty();
    }

    private Optional<Matcher<T, ?>> createSupplementaryMatcher(Rates rates) {
        if (rates instanceof RequestRates) {
            return requestMatcherFactory.of(((RequestRates)rates).getMatchConfig());
        }
        return Optional.empty();
    }

    private Matcher<T, ?> createElementMatcher(Element element) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(element);
        return new PathPatternsMatcher<>(pathPatterns, requestMatcherFactory);
    }
}
