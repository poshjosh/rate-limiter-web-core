package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.GenericDeclaration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class HttpRequestMatcherProvider implements MatcherProvider<HttpServletRequest> {

    private final UrlPathHelper urlPathHelper;
    
    private final ResourceInfoProvider resourceInfoProvider;
    private final ExpressionMatcher<HttpServletRequest, Object> expressionMatcher;

    HttpRequestMatcherProvider(
            String applicationPath,
            ResourceInfoProvider resourceInfoProvider,
            ExpressionMatcher<HttpServletRequest, Object> expressionMatcher) {
        this.resourceInfoProvider = Objects.requireNonNull(resourceInfoProvider);
        this.expressionMatcher = ExpressionMatcher.any(expressionMatcher, ExpressionMatcher.ofDefault());
        this.urlPathHelper = new UrlPathHelper(applicationPath);
    }

    @Override
    public Matcher<HttpServletRequest> createMainMatcher(RateConfig rateConfig) {
        final Rates rates = rateConfig.getRates();
        final RateSource source = rateConfig.getSource();
        Optional<Matcher<HttpServletRequest>> supplementaryMatcherOpt = createSupplementaryMatcher(rates);
        if (!source.isGroupType() && source.getSource() instanceof GenericDeclaration) {
            Matcher<HttpServletRequest> main = createWebRequestMatcher(source);
            if (!supplementaryMatcherOpt.isPresent()) {
                return main;
            }
            return main.and(supplementaryMatcherOpt.get());
        }
        return supplementaryMatcherOpt.orElse(Matcher.matchNone());
    }

    @Override
    public List<Matcher<HttpServletRequest>> createSubMatchers(RateConfig rateConfig) {
        return createSupplementaryMatchers(rateConfig.getRates());
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
        return expressionMatcher.matcher(expression);
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
