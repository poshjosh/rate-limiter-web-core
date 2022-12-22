package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.impl.DefaultWebRequestRateLimiterConfigBuilder;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public interface WebRequestRateLimiterConfig<REQUEST>{

    static <R> Builder<R> builder() {
        return new DefaultWebRequestRateLimiterConfigBuilder<>();
    }

    interface Builder<REQUEST> {

        WebRequestRateLimiterConfig<REQUEST> build();

        Builder<REQUEST> properties(RateLimitProperties properties);

        Builder<REQUEST> configurer(
                RateLimiterConfigurer<REQUEST> configurer);

        Builder<REQUEST> requestToIdConverter(
                RequestToIdConverter<REQUEST, String> requestToIdConverter);

        Builder<REQUEST> rateLimiterConfig(
                RateLimiterConfig<Object, Object> rateLimiterConfig);

        Builder<REQUEST> classIdProvider(IdProvider<Class<?>, String> classIdProvider);

        Builder<REQUEST> methodIdProvider(IdProvider<Method, String> methodIdProvider);

        Builder<REQUEST> classPathPatternsProvider(
                IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider);

        Builder<REQUEST> methodPathPatternsProvider(
                IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider);

        Builder<REQUEST> rateLimiterFactory(
                RateLimiterFactory<Object> rateLimiterFactory);

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

    Registries<REQUEST> getRegistries();

    MatcherRegistry<REQUEST> getMatcherRegistry();

    ResourceClassesSupplier getResourceClassesSupplier();

    RateLimitProperties getProperties();

    RateLimiterConfigurer<REQUEST> getConfigurer();

    RequestToIdConverter<REQUEST, String> getRequestToIdConverter();

    RateLimiterConfig<Object, Object> getRateLimiterConfig();

    IdProvider<Class<?>, String> getClassIdProvider();

    IdProvider<Method, String> getMethodIdProvider();

    IdProvider<Class<?>, PathPatterns<String>> getClassPathPatternsProvider();

    IdProvider<Method, PathPatterns<String>> getMethodPathPatternsProvider();

    RateLimiterFactory<Object> getRateLimiterFactory();

    ClassesInPackageFinder getClassesInPackageFinder();

    AnnotationProcessor<Class<?>, Rates> getAnnotationProcessor();

    Class<? extends Annotation>[] getResourceAnnotationTypes();

    NodeBuilder<List<Class<?>>, Rates> getNodeFactoryForAnnotations();

    NodeBuilder<RateLimitProperties, Rates> getNodeFactoryForProperties();
}
