package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsProvider;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.util.List;

public abstract class WebResourceLimiterConfig<REQUEST>{

    public static <R> Builder<R> builder() {
        return new WebResourceLimiterConfigBuilder<>();
    }

    public interface Builder<REQUEST> {

        WebResourceLimiterConfig<REQUEST> build();

        Builder<REQUEST> properties(RateLimitProperties properties);

        Builder<REQUEST> configurer(
                ResourceLimiterConfigurer<REQUEST> configurer);

        Builder<REQUEST> requestToIdConverter(
                RequestToIdConverter<REQUEST, String> requestToIdConverter);

        Builder<REQUEST> pathPatternsProvider(PathPatternsProvider classPathPatternsProvider);

        Builder<REQUEST> matcherFactory(MatcherFactory<REQUEST, Element> matcherFactory);

        Builder<REQUEST> resourceLimiterFactory(
                ResourceLimiterFactory<Object> resourceLimiterFactory);

        Builder<REQUEST> classesInPackageFinder(
                ClassesInPackageFinder classesInPackageFinder);

        Builder<REQUEST> annotationProcessor(
                AnnotationProcessor<Class<?>, Rates> annotationProcessor);

        Builder<REQUEST> resourceAnnotationTypes(
                Class<? extends Annotation>[] resourceAnnotationTypes);

        Builder<REQUEST> nodeFactoryForProperties(
                NodeBuilder<RateLimitProperties, Rates> nodeBuilderForProperties);

        Builder<REQUEST> nodeFactoryForAnnotations(
                NodeBuilder<List<Class<?>>, Rates> nodeBuilderForAnnotations);
    }

    public abstract RateLimitProperties getProperties();

    public abstract Registries<REQUEST> getRegistries();

    public List<Class<?>> getResourceClasses() {
        return getResourceClassesSupplier().get();
    }

    // Package access getters
    //
    abstract ResourceClassesSupplier getResourceClassesSupplier();

    abstract MatcherFactory<REQUEST, Element> getMatcherFactory();

    abstract NodeBuilder<List<Class<?>>, Rates> getNodeBuilderForAnnotations();

    abstract NodeBuilder<RateLimitProperties, Rates> getNodeBuilderForProperties();
}
