package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.Limit;
import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public interface WebRequestRateLimiterConfigBuilder<REQUEST> {

    WebRequestRateLimiterConfig<REQUEST> build();

    WebRequestRateLimiterConfigBuilder<REQUEST> properties(RateLimitProperties properties);

    WebRequestRateLimiterConfigBuilder<REQUEST> configurer(
            RateLimiterConfigurer<REQUEST> configurer);

    WebRequestRateLimiterConfigBuilder<REQUEST> requestToIdConverter(
            RequestToIdConverter<REQUEST, String> requestToIdConverter);

    WebRequestRateLimiterConfigBuilder<REQUEST> rateLimiterConfig(
            RateLimiterConfig<Object, Object> rateLimiterConfig);

    WebRequestRateLimiterConfigBuilder<REQUEST> classPathPatternsProvider(
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider);

    WebRequestRateLimiterConfigBuilder<REQUEST> methodPathPatternsProvider(
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider);

    WebRequestRateLimiterConfigBuilder<REQUEST> rateLimiterFactory(
            RateLimiterFactory<Object> rateLimiterFactory);

    WebRequestRateLimiterConfigBuilder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder);

    WebRequestRateLimiterConfigBuilder<REQUEST> annotationProcessor(
            AnnotationProcessor<Class<?>> annotationProcessor);

    WebRequestRateLimiterConfigBuilder<REQUEST> resourceAnnotationTypes(
            Class<? extends Annotation>[] resourceAnnotationTypes);

    WebRequestRateLimiterConfigBuilder<REQUEST> nodeFactoryForProperties(
            NodeFactory<RateLimitProperties, Limit> nodeFactoryForProperties);

    WebRequestRateLimiterConfigBuilder<REQUEST> nodeFactoryForAnnotations(
            NodeFactory<List<Class<?>>, Limit> nodeFactoryForAnnotations);
}
