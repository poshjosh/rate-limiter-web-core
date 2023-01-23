package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.Objects;
import java.util.Optional;

final class DefaultMatcherFactory<R> implements MatcherFactory<R> {

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestToIdConverter<R, String> requestToIdConverter;
    private final ExpressionMatcher<R, Object> expressionMatcher;

    private final ExpressionMatcher<R, Object> defaultExpressionMatcher;

    DefaultMatcherFactory(
            PathPatternsProvider pathPatternsProvider,
            RequestToIdConverter<R, String> requestToIdConverter,
            ExpressionMatcher<R, Object> expressionMatcher) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToIdConverter = Objects.requireNonNull(requestToIdConverter);
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);
        this.defaultExpressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override
    public Optional<Matcher<R, ?>> createMatcher(String name, RateConfig rateConfig) {
        final Object source = rateConfig.getSource();
        final Rates rates = rateConfig.getValue();
        Optional<Matcher<R, ?>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (source instanceof Element) {
            Matcher main = createPathPatternMatcher((Element)source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return Optional.of(main);
            }
            return Optional.of(main.andThen(supplementaryMatcherOpt.get()));
        }
        return supplementaryMatcherOpt;
    }

    private Optional<Matcher<R, ?>> createSupplementaryMatcher(Rates rates) {
        final String expression = rates.getRateCondition();
        if (expression != null && !expression.isEmpty()) {
            if (expressionMatcher.isSupported(expression)) {
                return Optional.of(expressionMatcher.with(expression));
            } else if (defaultExpressionMatcher.isSupported(expression)) {
                return Optional.of(defaultExpressionMatcher.with(expression));
            }
        }
        return Optional.empty();
    }

    private Matcher<R, ?> createPathPatternMatcher(Element element) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(element);
        return new PathPatternsMatcher<>(pathPatterns, requestToIdConverter);
    }

    /**
     * Matcher to match the path patterns declared on an element.
     *
     * @param <R> The type of the request for which a match will be checked for
     */
    private static class PathPatternsMatcher<R> implements Matcher<R, PathPatterns<String>> {

        private final PathPatterns<String> pathPatterns;

        private final RequestToIdConverter<R, String> requestToUriConverter;

        public PathPatternsMatcher(
                PathPatterns<String> pathPatterns,
                RequestToIdConverter<R, String> requestToUriConverter) {

            this.pathPatterns = Objects.requireNonNull(pathPatterns);

            this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        }

        @Override
        public PathPatterns<String> matchOrNull(R target) {
            return matches(target) ? pathPatterns : null;
        }

        @Override
        public boolean matches(R request) {
            String uri = requestToUriConverter.toId(request);
            return pathPatterns.matches(uri);
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            PathPatternsMatcher<?> that = (PathPatternsMatcher<?>)o;
            return pathPatterns.equals(that.pathPatterns);
        }

        @Override public int hashCode() {
            return Objects.hash(pathPatterns);
        }

        @Override
        public String toString() {
            return "PathPatternsMatcher{" + pathPatterns.getPatterns() + "}";
        }
    }
}
