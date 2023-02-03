package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class DefaultMatcherProvider<R, K extends Object> implements MatcherProvider<R, K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMatcherProvider.class);

    private final PathPatternsProvider pathPatternsProvider;
    private final RequestToIdConverter<R, String> requestToIdConverter;
    private final ExpressionMatcher<R, Object> expressionMatcher;

    private final ExpressionMatcher<R, Object> defaultExpressionMatcher;

    DefaultMatcherProvider(
            PathPatternsProvider pathPatternsProvider,
            RequestToIdConverter<R, String> requestToIdConverter,
            ExpressionMatcher<R, Object> expressionMatcher) {
        this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
        this.requestToIdConverter = Objects.requireNonNull(requestToIdConverter);
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);
        this.defaultExpressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override
    public Matcher<R, K> createMatcher(Node<RateConfig> node) {
        if (node.isRoot()) {
            return Matcher.matchNone();
        }
        final RateConfig rateConfig = requireRateConfig(node);
        final Rates rates = rateConfig.getRates();
        if(!rates.hasLimits() && !parentHasLimits(node)) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            return Matcher.matchNone();
        }
        final Object source = rateConfig.getSource();
        Optional<Matcher<R, String>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (source instanceof Element) {
            Matcher<R, PathPatterns<String>> main = createPathPatternMatcher((Element)source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return (Matcher<R, K>)main;
            }
            return main.andThen((Matcher)supplementaryMatcherOpt.get());
        }
        return (Matcher<R, K>)supplementaryMatcherOpt.get();
    }


    @Override
    public List<Matcher<R, K>> createMatchers(Node<RateConfig> node) {
        final Rates rates = requireRateConfig(node).getRates();
        return createSupplementaryMatchers(rates);
    }

    private boolean parentHasLimits(Node<RateConfig> node) {
        return node.getParentOptional()
                .filter(parent -> parent.hasNodeValue() && requireRates(parent).hasLimits())
                .isPresent();
    }

    private Rates requireRates(Node<RateConfig> node) {
        return Objects.requireNonNull(Checks.requireNodeValue(node).getRates());
    }

    private RateConfig requireRateConfig(Node<RateConfig> node) {
        return Objects.requireNonNull(node.getValueOrDefault(null));
    }

    private Optional<Matcher<R, String>> createSupplementaryMatcher(Rates rates) {
        return createExpressionMatcher(rates.getRateCondition());
    }

    private List<Matcher<R, K>> createSupplementaryMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createExpressionMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .map(matcher -> (Matcher<R, K>)matcher)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<R, String>> createExpressionMatcher(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Optional.empty();
        }
        if (expressionMatcher.isSupported(expression)) {
            return Optional.of(expressionMatcher.with(expression));
        } else if (defaultExpressionMatcher.isSupported(expression)) {
            return Optional.of(defaultExpressionMatcher.with(expression));
        }
        return Optional.empty();
    }

    private Matcher<R, PathPatterns<String>> createPathPatternMatcher(Element element) {
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
