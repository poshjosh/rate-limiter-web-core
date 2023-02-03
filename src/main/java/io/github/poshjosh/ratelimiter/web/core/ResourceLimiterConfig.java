package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;

public abstract class ResourceLimiterConfig<REQUEST>{

    public static Builder<HttpServletRequest> builderOfRequest() {
        return builder(HttpServletRequest.class);
    }

    public static <R> Builder<R> builder(Class<R> requestType) {
        return new ResourceLimiterConfigBuilder<>(requestType);
    }

    public interface Builder<REQUEST> {

        ResourceLimiterConfig<REQUEST> build();

        Builder<REQUEST> properties(RateLimitProperties properties);

        Builder<REQUEST> configurer(ResourceLimiterConfigurer<REQUEST> configurer);

        Builder<REQUEST> expressionMatcher(ExpressionMatcher<REQUEST, Object> expressionMatcher);

        Builder<REQUEST> requestToIdConverter(RequestToIdConverter<REQUEST, String> requestToIdConverter);

        Builder<REQUEST> pathPatternsProvider(PathPatternsProvider classPathPatternsProvider);

        Builder<REQUEST> classesInPackageFinder(
                ClassesInPackageFinder classesInPackageFinder);

        Builder<REQUEST> classRateProcessor(RateProcessor<Class<?>> rateProcessor);

        Builder<REQUEST> propertyRateProcessor(RateProcessor<RateLimitProperties> rateProcessor);
    }

    // Package access getters
    //
    abstract RateLimitProperties getProperties();

    abstract Optional<ResourceLimiterConfigurer<REQUEST>> getConfigurer();

    abstract Supplier<List<Class<?>>> getResourceClassesSupplier();

    abstract MatcherProvider<REQUEST, ?> getMatcherProvider();

    abstract RateProcessor<Class<?>> getClassRateProcessor();

    abstract RateProcessor<RateLimitProperties> getPropertyRateProcessor();

    Set<Class<?>> getResourceClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getProperties().getResourceClasses());
        classes.addAll(getResourceClassesSupplier().get());
        return Collections.unmodifiableSet(classes);
    }
}
