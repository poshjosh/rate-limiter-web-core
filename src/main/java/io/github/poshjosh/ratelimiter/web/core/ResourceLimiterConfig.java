package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.ArrayList;
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

        Builder<REQUEST> requestMatcherFactory(RequestMatcherFactory<REQUEST> requestMatcherFactory);

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
        List<Class<?>> classes = new ArrayList<>(getProperties().getResourceClasses());
        classes.addAll(getResourceClassesSupplier().get());
        return classes;
    }
}
