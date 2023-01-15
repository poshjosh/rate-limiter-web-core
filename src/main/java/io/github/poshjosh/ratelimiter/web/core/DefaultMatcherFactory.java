package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsMatcher;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<T> implements MatcherFactory<T> {

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestToIdConverter<T, String> requestToIdConverter;
    private final ExpressionMatcher<T, Object> expressionMatcher;

    private final ExpressionMatcher<T, Object> sysExpressionMatcher;

    DefaultMatcherFactory(
            PathPatternsProvider pathPatternsProvider,
            RequestToIdConverter<T, String> requestToIdConverter,
            ExpressionMatcher<T, Object> expressionMatcher) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToIdConverter = Objects.requireNonNull(requestToIdConverter);
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);
        this.sysExpressionMatcher = ExpressionMatcher.ofSystem();
    }

    @Override
    public Optional<Matcher<T, ?>> createMatcher(String name, RateConfig rateConfig) {
        final Object source = rateConfig.getSource();
        final Rates rates = rateConfig.getValue();
        Optional<Matcher<T, ?>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (source instanceof Element) {
            Matcher main = createElementMatcher((Element)source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return Optional.of(main);
            }
            return Optional.of(main.andThen(supplementaryMatcherOpt.get()));
        }
        return supplementaryMatcherOpt;
    }

    private Optional<Matcher<T, ?>> createSupplementaryMatcher(Rates rates) {
        final String expression = rates.getRateCondition();
        if (expression != null && !expression.isEmpty()) {
            if (expressionMatcher.isSupported(expression)) {
                return Optional.of(expressionMatcher.with(expression));
            } else if (sysExpressionMatcher.isSupported(expression)) {
                return Optional.of(sysExpressionMatcher.with(expression));
            }
        }
        return Optional.empty();
    }

    private Matcher<T, ?> createElementMatcher(Element element) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(element);
        return new PathPatternsMatcher<>(pathPatterns, requestToIdConverter);
    }
}
