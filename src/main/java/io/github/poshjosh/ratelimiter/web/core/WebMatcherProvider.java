package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatchers;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfo;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfos;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Objects;

final class WebMatcherProvider extends AbstractMatcherProvider<HttpServletRequest> {

    private final UrlPathHelper urlPathHelper;
    
    private final ResourceInfoProvider resourceInfoProvider;

    WebMatcherProvider(
            String applicationPath,
            ResourceInfoProvider resourceInfoProvider,
            ExpressionMatcher<HttpServletRequest> expressionMatcher) {
        super(ExpressionMatchers.any(expressionMatcher, ExpressionMatchers.ofDefaults()));
        this.resourceInfoProvider = Objects.requireNonNull(resourceInfoProvider);
        this.urlPathHelper = new UrlPathHelper(applicationPath);
    }

    @Override
    public Matcher<HttpServletRequest> createMainMatcher(RateConfig rateConfig) {
        final Rates rates = rateConfig.getRates();
        final RateSource source = rateConfig.getSource();
        final Matcher<HttpServletRequest> expressionMatcher =
                createExpressionMatcher(rates.getRateCondition()).orElse(null);
        if (isMatchNone(rateConfig, expressionMatcher != null)) {
            return Matchers.matchNone();
        }
        if (!source.isGroupType() && source.isGenericDeclaration()) {
            Matcher<HttpServletRequest> webRequestMatcher = createWebRequestMatcher(rateConfig);
            if (expressionMatcher == null) {
                return webRequestMatcher;
            }
            return webRequestMatcher.and(expressionMatcher);
        }
        return expressionMatcher == null ? Matchers.matchNone() : expressionMatcher;
    }

    @Override
    protected boolean isMatchNone(RateConfig rateConfig, boolean isExpressionPresent) {
        return super.isMatchNone(rateConfig, isExpressionPresent)
                && !rateConfig.shouldDelegateToParent();
    }

    private Matcher<HttpServletRequest> createWebRequestMatcher(RateConfig rateConfig) {
        final RateSource rateSource = rateConfig.getSource();
        ResourceInfo resourceInfo = resourceInfoProvider.get(rateSource);
        if (ResourceInfos.none().equals(resourceInfo)) {
            return Matchers.matchNone();
        }
        return new HttpRequestMatcher(rateConfig, urlPathHelper, resourceInfo);
    }

    /**
     * Matcher to match http request by (path patterns, request method etc) declared on an element.
     */
    private static class HttpRequestMatcher implements Matcher<HttpServletRequest> {
        private final RateConfig rateConfig;
        private final UrlPathHelper urlPathHelper;
        private final ResourceInfo resourceInfo;

        public HttpRequestMatcher(
                RateConfig rateConfig,
                UrlPathHelper urlPathHelper,
                ResourceInfo resourceInfo) {
            this.rateConfig = Objects.requireNonNull(rateConfig);
            this.urlPathHelper = Objects.requireNonNull(urlPathHelper);
            this.resourceInfo = Objects.requireNonNull(resourceInfo);
        }

        @Override
        public String match(HttpServletRequest target) {
            return matches(target) ? getId() : Matchers.NO_MATCH;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (!matchesHttpMethod(request.getMethod())) {
                return false;
            }
            return resourceInfo.getResourcePath().matches(getPathForMatching(request));
        }

        private String getId() {
            if (rateConfig.shouldDelegateToParent()) {
                final String parentId = rateConfig.getParent().getId();
                return parentId.isEmpty() ? resourceInfo.getId() : parentId;
            }
            return resourceInfo.getId();
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
        @Override
        public boolean equals(Object o) {
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
