package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;

public abstract class ResourceLimiterConfig {

    public static Builder builder() {
        return new ResourceLimiterConfigBuilder();
    }

    public interface Builder {

        ResourceLimiterConfig build();

        Builder properties(RateLimitProperties properties);

        Builder configurer(ResourceLimiterConfigurer configurer);

        Builder expressionMatcher(ExpressionMatcher<HttpServletRequest, Object> expressionMatcher);

        Builder resourceInfoProvider(ResourceInfoProvider resourceInfoProvider);

        Builder classesInPackageFinder(ClassesInPackageFinder classesInPackageFinder);

        Builder classRateProcessor(RateProcessor<Class<?>> rateProcessor);

        Builder propertyRateProcessor(RateProcessor<RateLimitProperties> rateProcessor);
    }

    // Package access getters
    //
    abstract RateLimitProperties getProperties();

    abstract Optional<ResourceLimiterConfigurer> getConfigurer();

    abstract Supplier<List<Class<?>>> getResourceClassesSupplier();

    abstract MatcherProvider<HttpServletRequest> getMatcherProvider();

    abstract RateProcessor<Class<?>> getClassRateProcessor();

    abstract RateProcessor<RateLimitProperties> getPropertyRateProcessor();

    Set<Class<?>> getResourceClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getProperties().getResourceClasses());
        classes.addAll(getResourceClassesSupplier().get());
        return Collections.unmodifiableSet(classes);
    }
}
