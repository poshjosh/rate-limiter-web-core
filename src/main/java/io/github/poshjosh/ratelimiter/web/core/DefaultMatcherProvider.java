package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.GenericDeclaration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class DefaultMatcherProvider implements MatcherProvider<HttpServletRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMatcherProvider.class);

    private final UrlPathHelper urlPathHelper;
    
    private final ResourceInfoProvider resourceInfoProvider;
    private final ExpressionMatcher<HttpServletRequest, Object> expressionMatcher;
    private final ExpressionMatcher<HttpServletRequest, Object> defaultExpressionMatcher;

    DefaultMatcherProvider(
            String applicationPath,
            ResourceInfoProvider resourceInfoProvider,
            ExpressionMatcher<HttpServletRequest, Object> expressionMatcher) {
        this.resourceInfoProvider = Objects.requireNonNull(resourceInfoProvider);
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);
        this.defaultExpressionMatcher = ExpressionMatcher.ofDefault();
        this.urlPathHelper = new UrlPathHelper(applicationPath);
    }

    @Override
    public Matcher<HttpServletRequest> createMatcher(Node<RateConfig> node) {
        RateConfig rateConfig = requireRateConfig(node);
        final Rates rates = rateConfig.getRates();
        if(!rates.hasLimits() && !parentHasLimits(node)) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            return Matcher.matchNone();
        }
        final RateSource source = rateConfig.getSource();
        Optional<Matcher<HttpServletRequest>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (!source.isGroupType() && source.getSource() instanceof GenericDeclaration) {
            Matcher<HttpServletRequest> main = createWebRequestMatcher(source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return main;
            }
            return main.andThen(supplementaryMatcherOpt.get());
        }
        return supplementaryMatcherOpt.orElse(Matcher.matchNone());
    }

    @Override
    public List<Matcher<HttpServletRequest>> createMatchers(Node<RateConfig> node) {
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

    private Optional<Matcher<HttpServletRequest>> createSupplementaryMatcher(Rates rates) {
        return createExpressionMatcher(rates.getRateCondition());
    }

    private List<Matcher<HttpServletRequest>> createSupplementaryMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createExpressionMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<HttpServletRequest>> createExpressionMatcher(String expression) {
        if (!StringUtils.hasText(expression)) {
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

    private Matcher<HttpServletRequest> createWebRequestMatcher(RateSource rateSource) {
        ResourceInfoProvider.ResourceInfo resourceInfo = resourceInfoProvider.get(rateSource);
        return new HttpRequestMatcher(urlPathHelper, resourceInfo);
    }

    /**
     * Matcher to match http request by (path patterns, request method etc) declared on an element.
     */
    private static class HttpRequestMatcher implements Matcher<HttpServletRequest> {

        private final UrlPathHelper urlPathHelper;

        private final ResourceInfoProvider.ResourceInfo resourceInfo;

        public HttpRequestMatcher(
                UrlPathHelper urlPathHelper,
                ResourceInfoProvider.ResourceInfo resourceInfo) {
            this.urlPathHelper = Objects.requireNonNull(urlPathHelper);
            this.resourceInfo = Objects.requireNonNull(resourceInfo);
        }

        @Override
        public String match(HttpServletRequest target) {
            return matches(target) ? resourceInfo.getId() : Matcher.NO_MATCH;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (!matchesHttpMethod(request.getMethod())) {
                return false;
            }
            return resourceInfo.getPathPatterns().matches(getPathForMatching(request));
        }

        /**
         * Get path for matching purposes.
         * @param request The HttpServletRequest for which a path will be returned
         * @return a path for matching purposes.
         */
        private String getPathForMatching(HttpServletRequest request) {
            return urlPathHelper.getPathWithinServlet(request);
        }

        private boolean matchesHttpMethod(String httpMethod) {
            Collection<String> httpMethods = resourceInfo.getHttpMethods();
            if (httpMethods.isEmpty()) { // If no method is defined, match all methods
                return true;
            }
            for(String method : httpMethods) {
                if (method.equalsIgnoreCase(httpMethod)) {
                    return true;
                }
            }
            return false;
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            HttpRequestMatcher that = (HttpRequestMatcher) o;
            return resourceInfo.equals(that.resourceInfo);
        }

        @Override public int hashCode() {
            return Objects.hash(resourceInfo);
        }

        @Override
        public String toString() {
            return "HttpRequestMatcher{" + resourceInfo + "}";
        }
    }
}
