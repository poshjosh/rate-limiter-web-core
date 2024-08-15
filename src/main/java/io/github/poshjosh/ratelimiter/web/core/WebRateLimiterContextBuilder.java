package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterContextImpl;
import io.github.poshjosh.ratelimiter.RateLimiterProvider;
import io.github.poshjosh.ratelimiter.annotation.RateProcessors;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.Ticker;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import java.util.*;

class WebRateLimiterContextBuilder implements WebRateLimiterContext.Builder {
    private final WebRateLimiterContextImpl context;
    WebRateLimiterContextBuilder() {
        this.context = new WebRateLimiterContextImpl();
    }

    @Override
    public WebRateLimiterContext build() {
        return context.withDefaultsAsFallback();
    }

    @Override public WebRateLimiterContext.Builder packages(String... packages) {
        context.setPackages(packages);
        return this;
    }

    @Override public WebRateLimiterContext.Builder classes(Class<?>... classes) {
        context.setClasses(classes);
        return this;
    }

    @Override public WebRateLimiterContext.Builder rates(
            List<Rates> rates) {
        context.setRates(rates);
        return this;
    }

    @Override public WebRateLimiterContext.Builder matcherProvider(
            MatcherProvider<RequestInfo> matcherProvider) {
        context.setMatcherProvider(matcherProvider);
        return this;
    }

    @Override public WebRateLimiterContext.Builder properties(
            RateLimitProperties properties) {
        context.setProperties(properties);
        return this;
    }

    @Override public WebRateLimiterContext.Builder configurer(
            RateLimiterConfigurer configurer) {
        context.configurer = configurer;
        return this;
    }

    @Override public WebRateLimiterContext.Builder classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        context.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public WebRateLimiterContext.Builder resourceInfoProvider(
            ResourceInfoProvider resourceInfoProvider) {
        context.resourceInfoProvider = resourceInfoProvider;
        return this;
    }

    @Override public WebRateLimiterContext.Builder expressionMatcher(
            ExpressionMatcher<RequestInfo> expressionMatcher) {
        context.expressionMatcher = expressionMatcher;
        return this;
    }

    @Override public WebRateLimiterContext.Builder classRateProcessor(
            RateProcessor<Class<?>> rateProcessor) {
        context.classRateProcessor = rateProcessor;
        return this;
    }

    @Override public WebRateLimiterContext.Builder propertyRateProcessor(
            RateProcessor<RateLimitProperties> rateProcessor) {
        context.propertyRateProcessor = rateProcessor;
        return this;
    }

    @Override public WebRateLimiterContext.Builder store(BandwidthsStore<String> store) {
        context.setStore(store);
        return this;
    }

    @Override public WebRateLimiterContext.Builder rateLimiterProvider(
            RateLimiterProvider rateLimiterProvider) {
        context.setRateLimiterProvider(rateLimiterProvider);
        return this;
    }

    @Override public WebRateLimiterContext.Builder ticker(Ticker ticker) {
        context.setTicker(ticker);
        return this;
    }

    private static final class WebRateLimiterContextImpl extends RateLimiterContextImpl<RequestInfo> implements WebRateLimiterContext {

        private RateLimiterConfigurer configurer;
        private ClassesInPackageFinder classesInPackageFinder;
        private ResourceInfoProvider resourceInfoProvider;
        private ExpressionMatcher<RequestInfo> expressionMatcher;
        private RateProcessor<Class<?>> classRateProcessor;
        private RateProcessor<RateLimitProperties> propertyRateProcessor;

        private WebRateLimiterContextImpl() {}

        @Override public WebRateLimiterContext withDefaultsAsFallback() {

            if (getProperties() == null) {
                // We allow empty properties
                setProperties(new EmptyRateLimitProperties());
            }

            if (expressionMatcher == null) {
                expressionMatcher = new WebExpressionMatcher();
            }
            if (classesInPackageFinder == null) {
                classesInPackageFinder = (ClassesInPackageFinder.ofDefaults());
            }
            if (classRateProcessor == null) {
                // We accept all class/method  nodes, even those without rate limit related annotations
                // This is because, any of the nodes may have its rate limit related info, specified
                // via properties. Such a node needs to be accepted at this point as property
                // sourced rate limited data will later be transferred to class/method nodes
                classRateProcessor = (RateProcessors.ofClass(source -> true));
            }

            if (propertyRateProcessor == null) {
                propertyRateProcessor = (RateProcessors.ofProperties());
            }

            if (getMatcherProvider() == null) {
                MatcherProvider<RequestInfo> matcherProvider = new WebMatcherProvider(
                        getProperties().getApplicationPath(),
                        resourceInfoProvider,
                        expressionMatcher);
                setMatcherProvider(matcherProvider);
            }

            super.withDefaultsAsFallback();

            return this;
        }

        @Override public ClassesInPackageFinder getClassesInPackageFinder() {
            return classesInPackageFinder;
        }

        @Override public Optional<RateLimiterConfigurer> getConfigurerOptional() {
            return Optional.ofNullable(configurer);
        }
    }

    private static final class EmptyRateLimitProperties implements RateLimitProperties {
        private EmptyRateLimitProperties() { }
        @Override public List<Class<?>> getResourceClasses() { return Collections.emptyList(); }
        @Override public List<String> getResourcePackages() {
            return Collections.emptyList();
        }
    }
}
