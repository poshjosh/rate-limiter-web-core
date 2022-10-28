package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public interface WebRequestRateLimiterConfig<REQUEST> {

    RateLimiterRegistry<REQUEST> getRateLimiterRegistry();

    MatcherRegistry<REQUEST> getMatcherRegistry();

    ResourceClassesSupplier getResourceClassesSupplier();

    RateLimitProperties getProperties();

    RateLimiterConfigurer<REQUEST> getConfigurer();

    RequestToIdConverter<REQUEST, String> getRequestToIdConverter();

    RateLimiterConfig<Object, Object> getRateLimiterConfig();

    IdProvider<Class<?>, PathPatterns<String>> getClassPathPatternsProvider();

    IdProvider<Method, PathPatterns<String>> getMethodPathPatternsProvider();

    RateLimiterFactory<Object> getRateLimiterFactory();

    ClassesInPackageFinder getClassesInPackageFinder();

    AnnotationProcessor<Class<?>> getAnnotationProcessor();

    Class<? extends Annotation>[] getResourceAnnotationTypes();

    NodeFactory<List<Class<?>>, RateConfigList> getNodeFactoryForAnnotations();

    NodeFactory<RateLimitProperties, RateConfigList> getNodeFactoryForProperties();

    PatternMatchingRateLimiterFactory<REQUEST> getPatternMatchingRateLimiterFactoryForProperties();

    PatternMatchingRateLimiterFactory<REQUEST> getPatternMatchingRateLimiterFactoryForAnnotations();
}
