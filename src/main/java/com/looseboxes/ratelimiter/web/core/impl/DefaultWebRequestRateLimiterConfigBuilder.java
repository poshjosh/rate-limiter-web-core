package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.ClassAnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.DefaultClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.*;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * We thought of using the builder pattern. However, if a hundred RateLimiters are created,
 * then multiple objects will end up being created for each instance of RateLimiter, when
 * using the builder pattern.
 * @param <REQUEST> The type of the request/request related object
 */
public class DefaultWebRequestRateLimiterConfigBuilder<REQUEST>
        implements WebRequestRateLimiterConfigBuilder<REQUEST> {

    private static class WebRequestRateLimiterConfigImpl<T> implements
            WebRequestRateLimiterConfig<T> {

        private RateLimitProperties properties;
        @Nullable private RateLimiterConfigurer<T> configurer;
        private RequestToIdConverter<T, String> requestToIdConverter;
        private RateLimiterConfig<Object, Object> rateLimiterConfig;
        private IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;
        private IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;
        private RateLimiterFactory<Object> rateLimiterFactory;
        private ClassesInPackageFinder classesInPackageFinder;
        private AnnotationProcessor<Class<?>> annotationProcessor;
        private Class<? extends Annotation> [] resourceAnnotationTypes;

        private RateLimiterRegistry<T> rateLimiterRegistry;
        private MatcherRegistry<T> matcherRegistry;
        private ResourceClassesSupplier resourceClassesSupplier;

        private NodeFactory<RateLimitProperties, Limit> nodeFactoryForProperties;
        private NodeFactory<List<Class<?>>, Limit> nodeFactoryForAnnotations;
        private PatternMatchingRateLimiterFactory<T> patternMatchingRateLimiterFactoryForProperties;
        private PatternMatchingRateLimiterFactory<T> patternMatchingRateLimiterFactoryForAnnotations;

        @Override public RateLimitProperties getProperties() {
            return properties;
        }

        @Override public RateLimiterConfigurer<T> getConfigurer() {
            return configurer;
        }

        @Override public RequestToIdConverter<T, String> getRequestToIdConverter() {
            return requestToIdConverter;
        }

        @Override public RateLimiterConfig<Object, Object> getRateLimiterConfig() {
            return rateLimiterConfig;
        }

        @Override public IdProvider<Class<?>, PathPatterns<String>> getClassPathPatternsProvider() {
            return classPathPatternsProvider;
        }

        @Override public IdProvider<Method, PathPatterns<String>> getMethodPathPatternsProvider() {
            return methodPathPatternsProvider;
        }

        @Override public RateLimiterFactory<Object> getRateLimiterFactory() {
            return rateLimiterFactory;
        }

        @Override public ClassesInPackageFinder getClassesInPackageFinder() {
            return classesInPackageFinder;
        }

        @Override public AnnotationProcessor<Class<?>> getAnnotationProcessor() {
            return annotationProcessor;
        }

        @Override public Class<? extends Annotation>[] getResourceAnnotationTypes() {
            return resourceAnnotationTypes;
        }

        @Override public RateLimiterRegistry<T> getRateLimiterRegistry() {
            return rateLimiterRegistry;
        }

        @Override public MatcherRegistry<T> getMatcherRegistry() {
            return matcherRegistry;
        }

        @Override public ResourceClassesSupplier getResourceClassesSupplier() {
            return resourceClassesSupplier;
        }

        @Override public NodeFactory<RateLimitProperties, Limit> getNodeFactoryForProperties() {
            return nodeFactoryForProperties;
        }

        @Override public NodeFactory<List<Class<?>>, Limit> getNodeFactoryForAnnotations() {
            return nodeFactoryForAnnotations;
        }

        @Override public PatternMatchingRateLimiterFactory<T> getPatternMatchingRateLimiterFactoryForProperties() {
            return patternMatchingRateLimiterFactoryForProperties;
        }

        @Override public PatternMatchingRateLimiterFactory<T> getPatternMatchingRateLimiterFactoryForAnnotations() {
            return patternMatchingRateLimiterFactoryForAnnotations;
        }
    }

    private final WebRequestRateLimiterConfigImpl<REQUEST> configuration;

    public DefaultWebRequestRateLimiterConfigBuilder() {
        this.configuration = new WebRequestRateLimiterConfigImpl<>();
    }

    @Override public WebRequestRateLimiterConfig<REQUEST> build() {
        if (configuration.resourceAnnotationTypes.length == 0) {
            throw new IndexOutOfBoundsException("Index: 0");
        }
        if (configuration.rateLimiterConfig == null) {
            rateLimiterConfig(new DefaultRateLimiterConfig<>());
        }
        if (configuration.rateLimiterFactory == null) {
            rateLimiterFactory(new DefaultRateLimiterFactory<>());
        }
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(new DefaultClassesInPackageFinder());
        }
        if (configuration.annotationProcessor == null) {
            annotationProcessor(new ClassAnnotationProcessor());
        }
        if (configuration.nodeFactoryForProperties == null) {
            nodeFactoryForProperties(new NodeFromPropertiesFactory());
        }
        if (configuration.nodeFactoryForAnnotations == null) {
            nodeFactoryForAnnotations(new NodeFromAnnotationsFactory(configuration.annotationProcessor));
        }
        configuration.matcherRegistry = new DefaultMatcherRegistry<>(
                configuration.requestToIdConverter, configuration.classPathPatternsProvider, configuration.methodPathPatternsProvider);
        configuration.rateLimiterRegistry = new DefaultRateLimiterRegistry<>(
                configuration.matcherRegistry, configuration.rateLimiterConfig, configuration.rateLimiterFactory, configuration.configurer);
        configuration.resourceClassesSupplier = new DefaultResourceClassesSupplier(
                configuration.classesInPackageFinder, configuration.properties.getResourcePackages(), configuration.resourceAnnotationTypes);

        configuration.patternMatchingRateLimiterFactoryForProperties =
                new DefaultPatternMatchingRateLimiterFactory<>(
                        configuration.properties,
                        configuration.nodeFactoryForProperties,
                        configuration.rateLimiterRegistry,
                        (src, node) -> { });

        configuration.patternMatchingRateLimiterFactoryForAnnotations =
                new DefaultPatternMatchingRateLimiterFactory<>(
                        configuration.resourceClassesSupplier.get(),
                        configuration.nodeFactoryForAnnotations,
                        configuration.rateLimiterRegistry,
                        (src, node) -> { });

        return configuration;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> properties(
            RateLimitProperties properties) {
        configuration.properties = properties;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> configurer(
            @Nullable RateLimiterConfigurer<REQUEST> configurer) {
        configuration.configurer = configurer;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> requestToIdConverter(
            RequestToIdConverter<REQUEST, String> requestToIdConverter) {
        configuration.requestToIdConverter = requestToIdConverter;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> rateLimiterConfig(
            RateLimiterConfig<Object, Object> rateLimiterConfig) {
        configuration.rateLimiterConfig = rateLimiterConfig;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> classPathPatternsProvider(
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider) {
        configuration.classPathPatternsProvider = classPathPatternsProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> methodPathPatternsProvider(
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        configuration.methodPathPatternsProvider = methodPathPatternsProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> rateLimiterFactory(
            RateLimiterFactory<Object> rateLimiterFactory) {
        configuration.rateLimiterFactory = rateLimiterFactory;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> annotationProcessor(
            AnnotationProcessor<Class<?>> annotationProcessor) {
        configuration.annotationProcessor = annotationProcessor;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> resourceAnnotationTypes(
            Class<? extends Annotation>[] resourceAnnotationTypes) {
        configuration.resourceAnnotationTypes = resourceAnnotationTypes;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> nodeFactoryForProperties(
            NodeFactory<RateLimitProperties, Limit> nodeFactoryForProperties) {
        configuration.nodeFactoryForProperties = nodeFactoryForProperties;
        return this;
    }

    @Override public WebRequestRateLimiterConfigBuilder<REQUEST> nodeFactoryForAnnotations(
            NodeFactory<List<Class<?>>, Limit> nodeFactoryForAnnotations) {
        configuration.nodeFactoryForAnnotations = nodeFactoryForAnnotations;
        return this;
    }
}
