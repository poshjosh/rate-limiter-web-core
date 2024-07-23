package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterContext;
import io.github.poshjosh.ratelimiter.RateLimiterProvider;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.util.Ticker;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public interface WebRateLimiterContext extends RateLimiterContext<HttpServletRequest> {

    /**
     * Users of the returned builder are required (at the minimum) to provide:
     * {@link Builder#resourceInfoProvider(ResourceInfoProvider)}
     * @return A builder for {@link WebRateLimiterContext}
     */
    static Builder builder() {
        return new WebRateLimiterContextBuilder();
    }

    /**
     * Users are required (at the minimum) to provide: {@link Builder#resourceInfoProvider(ResourceInfoProvider)}
     */
    interface Builder extends RateLimiterContext.Builder<HttpServletRequest> {

        WebRateLimiterContext build();

        /**
         * <p><b>Not mandatory</b></p>
         * @param configurer The configurer for fine-grained configuration of rate limiting
         * @return this builder
         */
        Builder configurer(RateLimiterConfigurer configurer);

        /**
         * <p><b>Not mandatory</b></p>
         * @param classesInPackageFinder For locating classes in named packages
         * @return this builder
         */
        Builder classesInPackageFinder(ClassesInPackageFinder classesInPackageFinder);

        /**
         * <p><b>Not mandatory</b></p>
         * @param expressionMatcher For matching rate condition expressions
         * @return this builder
         */
        Builder expressionMatcher(ExpressionMatcher<HttpServletRequest> expressionMatcher);

        /**
         * <p><b>Mandatory</b></p>
         * @param resourceInfoProvider For extracting resource info from javax.servlet.http.HttpServletRequests
         * @return this builder
         */
        Builder resourceInfoProvider(ResourceInfoProvider resourceInfoProvider);

        /**
         * <p><b>Not mandatory</b></p>
         * @param rateProcessor For processing rates specified via Classes and their members
         * @return this builder
         */
        Builder classRateProcessor(RateProcessor<Class<?>> rateProcessor);

        /**
         * <p><b>Not mandatory</b></p>
         * @param rateProcessor For processing rates specified via io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperites
         * @return this builder
         */
        Builder propertyRateProcessor(RateProcessor<RateLimitProperties> rateProcessor);

        @Override Builder packages(String... packages);

        @Override Builder classes(Class<?>... classes);

        @Override Builder rates(Map<String, Rates> rates);

        @Override Builder properties(RateLimitProperties properties);

        @Override Builder matcherProvider(MatcherProvider<HttpServletRequest> matcherProvider);

        @Override Builder rateLimiterProvider(RateLimiterProvider rateLimiterProvider);

        @Override Builder store(BandwidthsStore<?> store);

        @Override Builder ticker(Ticker ticker);
    }

    Optional<RateLimiterConfigurer> getConfigurerOptional();
}
