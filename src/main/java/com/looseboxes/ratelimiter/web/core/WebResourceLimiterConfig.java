package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.ResourceLimiterConfig;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.impl.WebResourceLimiterConfigBuilder;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public interface WebResourceLimiterConfig<REQUEST>{

    static <R> Builder<R> builder() {
        return new WebResourceLimiterConfigBuilder<>();
    }

    interface Builder<REQUEST> {

        WebResourceLimiterConfig<REQUEST> build();

        Builder<REQUEST> properties(RateLimitProperties properties);

        Builder<REQUEST> configurer(
                ResourceLimiterConfigurer<REQUEST> configurer);

        Builder<REQUEST> requestToIdConverter(
                RequestToIdConverter<REQUEST, String> requestToIdConverter);

        Builder<REQUEST> rateLimiterConfig(
                ResourceLimiterConfig<Object, Object> resourceLimiterConfig);

        Builder<REQUEST> classIdProvider(IdProvider<Class<?>, String> classIdProvider);

        Builder<REQUEST> methodIdProvider(IdProvider<Method, String> methodIdProvider);

        Builder<REQUEST> classPathPatternsProvider(
                IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider);

        Builder<REQUEST> methodPathPatternsProvider(
                IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider);

        Builder<REQUEST> classMatcherFactory(MatcherFactory<REQUEST, Class<?>> matcherFactory);

        Builder<REQUEST> methodMatcherFactory(MatcherFactory<REQUEST, Method> matcherFactory);

        Builder<REQUEST> rateLimiterFactory(
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

    Registries<REQUEST> getRegistries();

    default List<Class<?>> getResourceClasses() {
        return getResourceClassesSupplier().get();
    }

    ResourceClassesSupplier getResourceClassesSupplier();

    RateLimitProperties getProperties();

    //We don't want this exposed this way.
    //A user could manually call getConfigurer()#configure() expecting some meaningful side effects
    //The configure method is called once behind the scenes. The user should never have to.
    //
    //ResourceLimiterConfigurer<REQUEST> getConfigurer();

    RequestToIdConverter<REQUEST, String> getRequestToIdConverter();

    ResourceLimiterConfig<Object, Object> getRateLimiterConfig();

    IdProvider<Class<?>, String> getClassIdProvider();

    IdProvider<Method, String> getMethodIdProvider();

    IdProvider<Class<?>, PathPatterns<String>> getClassPathPatternsProvider();

    IdProvider<Method, PathPatterns<String>> getMethodPathPatternsProvider();

    MatcherFactory<REQUEST, Class<?>> getClassMatcherFactory();

    MatcherFactory<REQUEST, Method> getMethodMatcherFactory();

    ResourceLimiterFactory<Object> getRateLimiterFactory();

    ClassesInPackageFinder getClassesInPackageFinder();

    AnnotationProcessor<Class<?>, Rates> getAnnotationProcessor();

    Class<? extends Annotation>[] getResourceAnnotationTypes();

    NodeBuilder<List<Class<?>>, Rates> getNodeBuilderForAnnotations();

    NodeBuilder<RateLimitProperties, Rates> getNodeBuilderForProperties();
}
