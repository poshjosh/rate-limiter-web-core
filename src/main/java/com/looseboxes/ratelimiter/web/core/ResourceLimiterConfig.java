package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsProvider;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class ResourceLimiterConfig<REQUEST>{

    public static <R> Builder<R> builder() {
        return new ResourceLimiterConfigBuilder<>();
    }

    public interface Builder<REQUEST> {

        ResourceLimiterConfig<REQUEST> build();

        Builder<REQUEST> properties(RateLimitProperties properties);

        Builder<REQUEST> configurer(ResourceLimiterConfigurer<REQUEST> configurer);

        Builder<REQUEST> requestToIdConverter(
                RequestToIdConverter<REQUEST, String> requestToIdConverter);

        Builder<REQUEST> pathPatternsProvider(PathPatternsProvider classPathPatternsProvider);

        Builder<REQUEST> matcherFactory(MatcherFactory<REQUEST> matcherFactory);

        Builder<REQUEST> resourceLimiterFactory(
                ResourceLimiterFactory<Object> resourceLimiterFactory);

        Builder<REQUEST> classesInPackageFinder(
                ClassesInPackageFinder classesInPackageFinder);

        Builder<REQUEST> annotationProcessor(AnnotationProcessor<Class<?>> annotationProcessor);
    }

    // Package access getters
    //
    abstract RateLimitProperties getProperties();

    abstract Optional<ResourceLimiterConfigurer<REQUEST>> getConfigurer();

    abstract Supplier<List<Class<?>>> getResourceClassesSupplier();

    abstract MatcherFactory<REQUEST> getMatcherFactory();

    abstract ResourceLimiterFactory<Object> getResourceLimiterFactory();

    abstract AnnotationProcessor<Class<?>> getAnnotationProcessor();

    List<Class<?>> getResourceClasses() {
        return getResourceClassesSupplier().get();
    }
}
