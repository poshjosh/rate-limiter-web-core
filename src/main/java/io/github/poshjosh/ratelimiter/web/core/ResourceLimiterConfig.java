package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterProvider;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;

public interface ResourceLimiterConfig {

    /**
     * Users of the returned builder are required (at the minimum) to provide:
     * {@link Builder#resourceInfoProvider(ResourceInfoProvider)}
     * @return A builder for {@link ResourceLimiterConfig}
     */
    static Builder builder() {
        return new ResourceLimiterConfigBuilder();
    }

    /**
     * Users are required (at the minimum) to provide: {@link Builder#resourceInfoProvider(ResourceInfoProvider)}
     */
    interface Builder {

        ResourceLimiterConfig build();

        /**
         * <p><b>Not mandatory</b></p>
         * @param properties The properties containing rate limit specifications
         * @return this builder
         */
        Builder properties(RateLimitProperties properties);

        /**
         * <p><b>Not mandatory</b></p>
         * @param configurer The configurer for fine-grained configuration of rate limiting
         * @return this builder
         */
        Builder configurer(ResourceLimiterConfigurer configurer);

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
        Builder expressionMatcher(ExpressionMatcher<HttpServletRequest, Object> expressionMatcher);

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

        /**
         * <p><b>Not mandatory.</b> If not specified an in-memory instance is used</p>
         * @param store For storing bandwidths
         * @return this builder
         */
        Builder store(BandwidthsStore<?> store);

        Builder addUsageListener(UsageListener listener);

        /**
         * <p><b>Not mandatory</b></p>
         * @param listener Listener for usage of rate limited resources
         * @return this builder
         */
        Builder usageListener(UsageListener listener);

        /**
         * <p><b>Not mandatory</b></p>
         * @param rateLimiterProvider For provider rate limiters
         * @return this builder
         */
        Builder rateLimiterProvider(RateLimiterProvider<HttpServletRequest, ?> rateLimiterProvider);
    }

    RateLimitProperties getProperties();

    Optional<ResourceLimiterConfigurer> getConfigurer();

    ClassesInPackageFinder getClassesInPackageFinder();

    default Supplier<Set<Class<?>>> getResourceClassesSupplier() {
        return () -> {
            Set<Class<?>> classes = new HashSet<>();
            classes.addAll(getProperties().getResourceClasses());
            classes.addAll(getClassesInPackageFinder()
                    .findClasses(getProperties().getResourcePackages()));
            return Collections.unmodifiableSet(classes);
        };
    }

    MatcherProvider<HttpServletRequest> getMatcherProvider();

    RateProcessor<Class<?>> getClassRateProcessor();

    RateProcessor<RateLimitProperties> getPropertyRateProcessor();

    BandwidthsStore<?> getStore();

    UsageListener getUsageListener();

    RateLimiterProvider<HttpServletRequest, ?> getRateLimiterProvider();
}
