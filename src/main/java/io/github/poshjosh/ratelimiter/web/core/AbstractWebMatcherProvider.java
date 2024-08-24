package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatchers;
import io.github.poshjosh.ratelimiter.matcher.Matcher;
import io.github.poshjosh.ratelimiter.matcher.Matchers;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.AbstractMatcherProvider;

public abstract class AbstractWebMatcherProvider extends AbstractMatcherProvider<RequestInfo> {

    protected AbstractWebMatcherProvider() {
        super(ExpressionMatchers.ofDefaults());
    }

    protected AbstractWebMatcherProvider(ExpressionMatcher<RequestInfo> expressionMatcher) {
        super(ExpressionMatchers.any(expressionMatcher, ExpressionMatchers.ofDefaults()));
    }

    protected abstract Matcher<RequestInfo> createWebRequestMatcher(RateConfig rateConfig);

    @Override
    public Matcher<RequestInfo> createMainMatcher(RateConfig rateConfig) {
        final Rates rates = rateConfig.getRates();
        final RateSource source = rateConfig.getSource();
        final Matcher<RequestInfo> expressionMatcher =
                createExpressionMatcher(rates.getCondition()).orElse(null);
        if (isMatchNone(rateConfig, expressionMatcher != null)) {
            return Matchers.matchNone();
        }
        if (isWebType(source)) {
            Matcher<RequestInfo> webRequestMatcher = createWebRequestMatcher(rateConfig);
            if (expressionMatcher == null) {
                return webRequestMatcher;
            }
            return webRequestMatcher.and(expressionMatcher);
        }
        return expressionMatcher == null ? Matchers.matchNone() : expressionMatcher;
    }

    protected boolean isWebType(RateSource source) {
        return !source.isGroupType() && source.isGenericDeclaration();
    }

    @Override
    protected boolean isMatchNone(RateConfig rateConfig, boolean isExpressionPresent) {
        return super.isMatchNone(rateConfig, isExpressionPresent)
                && !rateConfig.shouldDelegateToParent();
    }
}
