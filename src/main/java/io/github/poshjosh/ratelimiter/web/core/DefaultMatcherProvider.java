package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class DefaultMatcherProvider<R> implements MatcherProvider<R> {

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
    public Matcher<R> createMatcher(Node<RateConfig> node) {
        RateConfig rateConfig = requireRateConfig(node);
        final Rates rates = rateConfig.getRates();
        if(!rates.hasLimits() && !parentHasLimits(node)) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            return Matcher.matchNone();
        }
        final RateSource source = rateConfig.getSource();
        Optional<Matcher<R>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (!source.isGroupType() && source.getSource() instanceof GenericDeclaration) {
            Matcher<R> main = createPathPatternMatcher(source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return main;
            }
            return main.andThen(supplementaryMatcherOpt.get());
        }
        return supplementaryMatcherOpt.orElse(Matcher.matchNone());
    }

    @Override
    public List<Matcher<R>> createMatchers(Node<RateConfig> node) {
        RateConfig rateConfig = requireRateConfig(node);
        return createSupplementaryMatchers(rateConfig.getRates());
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

    private Optional<Matcher<R>> createSupplementaryMatcher(Rates rates) {
        return createExpressionMatcher(rates.getRateCondition());
    }

    private List<Matcher<R>> createSupplementaryMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createExpressionMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<R>> createExpressionMatcher(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Optional.empty();
        }
        if (expressionMatcher.isSupported(expression)) {
            return Optional.of(expressionMatcher.with(expression));
        } else if (defaultExpressionMatcher.isSupported(expression)) {
            return Optional.of(defaultExpressionMatcher.with(expression));
        }
        throw new UnsupportedOperationException("Expression not supported: " + expression +
                " by any of: [" + expressionMatcher.getClass().getSimpleName() +
                "," + expressionMatcher.getClass().getSimpleName() + "]");
    }

    private Matcher<R> createPathPatternMatcher(RateSource rateSource) {
        PathPatterns<String> pathPatterns = pathPatternsProvider.get(rateSource);
        return new PathPatternsMatcher<>(pathPatterns, requestToIdConverter);
    }

    /**
     * Matcher to match the path patterns declared on an element.
     *
     * @param <R> The type of the request for which a match will be checked for
     */
    private static class PathPatternsMatcher<R> implements Matcher<R> {

        private final PathPatterns<String> pathPatterns;

        private final RequestToIdConverter<R, String> requestToUriConverter;

        private final String id;

        public PathPatternsMatcher(
                PathPatterns<String> pathPatterns,
                RequestToIdConverter<R, String> requestToUriConverter) {

            this.pathPatterns = Objects.requireNonNull(pathPatterns);

            this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);

            final List<String> paths = pathPatterns.getPatterns();
            this.id = paths.isEmpty() ? "" : (paths.size() == 1 ? paths.get(0) : paths.toString());
        }

        @Override
        public String match(R target) {
            return matches(target) ? id : Matcher.NO_MATCH;
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
