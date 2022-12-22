package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.*;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @param <REQUEST> The type of the request related object
 */
public class DefaultWebRequestRateLimiterConfigBuilder<REQUEST>
        implements WebRequestRateLimiterConfig.Builder<REQUEST> {

    private static class WebRequestRateLimiterConfigImpl<T> implements
            WebRequestRateLimiterConfig<T> {

        private RateLimitProperties properties;
        @Nullable private RateLimiterConfigurer<T> configurer;
        private RequestToIdConverter<T, String> requestToIdConverter;
        private RateLimiterConfig<Object, Object> rateLimiterConfig;
        private IdProvider<Class<?>, String> classIdProvider;
        private IdProvider<Method, String> methodIdProvider;
        private IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;
        private IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;
        private RateLimiterFactory<Object> rateLimiterFactory;
        private ClassesInPackageFinder classesInPackageFinder;
        private AnnotationProcessor<Class<?>, RateLimitConfig> annotationProcessor;
        private Class<? extends Annotation> [] resourceAnnotationTypes;

        private Registries<T> registries;
        private MatcherRegistry<T> matcherRegistry;
        private ResourceClassesSupplier resourceClassesSupplier;

        private NodeBuilder<RateLimitProperties, RateLimitConfig> nodeBuilderForProperties;
        private NodeBuilder<List<Class<?>>, RateLimitConfig> nodeBuilderForAnnotations;

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

        @Override public IdProvider<Class<?>, String> getClassIdProvider() {
            return classIdProvider;
        }

        @Override public IdProvider<Method, String> getMethodIdProvider() {
            return methodIdProvider;
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

        @Override public AnnotationProcessor<Class<?>, RateLimitConfig> getAnnotationProcessor() {
            return annotationProcessor;
        }

        @Override public Class<? extends Annotation>[] getResourceAnnotationTypes() {
            return resourceAnnotationTypes;
        }

        @Override public Registries<T> getRegistries() {
            return registries;
        }

        @Override public MatcherRegistry<T> getMatcherRegistry() {
            return matcherRegistry;
        }

        @Override public ResourceClassesSupplier getResourceClassesSupplier() {
            return resourceClassesSupplier;
        }

        @Override public NodeBuilder<RateLimitProperties, RateLimitConfig> getNodeFactoryForProperties() {
            return nodeBuilderForProperties;
        }

        @Override public NodeBuilder<List<Class<?>>, RateLimitConfig> getNodeFactoryForAnnotations() {
            return nodeBuilderForAnnotations;
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
            rateLimiterConfig(RateLimiterConfig.newInstance());
        }
        if (configuration.rateLimiterFactory == null) {
            rateLimiterFactory(RateLimiterFactory.newInstance());
        }
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(ClassesInPackageFinder.newInstance());
        }
        if (configuration.classIdProvider == null) {
            configuration.classIdProvider = IdProvider.forClass();
        }
        if (configuration.methodIdProvider == null) {
            configuration.methodIdProvider = IdProvider.forMethod();
        }
        if (configuration.annotationProcessor == null) {
            annotationProcessor(AnnotationProcessor.newInstance(
                    configuration.classIdProvider, configuration.methodIdProvider,
                    new AnnotationToRateLimitConfigConverter()));
        }
        if (configuration.nodeBuilderForProperties == null) {
            nodeFactoryForProperties(new PropertiesToRateLimitConfigNodeBuilder());
        }
        if (configuration.nodeBuilderForAnnotations == null) {
            nodeFactoryForAnnotations(new AnnotationToRateLimitConfigNodeBuilder(configuration.annotationProcessor));
        }

        configuration.matcherRegistry = new DefaultMatcherRegistry<>(
                configuration.requestToIdConverter,
                configuration.classIdProvider, configuration.methodIdProvider,
                configuration.classPathPatternsProvider, configuration.methodPathPatternsProvider);

        configuration.registries = new DefaultRegistries<>(
                configuration.classIdProvider, configuration.methodIdProvider,
                configuration.matcherRegistry, configuration.rateLimiterConfig,
                configuration.rateLimiterFactory, configuration.configurer);
        configuration.resourceClassesSupplier = new DefaultResourceClassesSupplier(
                configuration.classesInPackageFinder, configuration.properties.getResourcePackages(), configuration.resourceAnnotationTypes);

        return configuration;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> properties(
            RateLimitProperties properties) {
        configuration.properties = properties;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> configurer(
            @Nullable RateLimiterConfigurer<REQUEST> configurer) {
        configuration.configurer = configurer;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> requestToIdConverter(
            RequestToIdConverter<REQUEST, String> requestToIdConverter) {
        configuration.requestToIdConverter = requestToIdConverter;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> rateLimiterConfig(
            RateLimiterConfig<Object, Object> rateLimiterConfig) {
        configuration.rateLimiterConfig = rateLimiterConfig;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> classIdProvider(
            IdProvider<Class<?>, String> classIdProvider) {
        configuration.classIdProvider = classIdProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> methodIdProvider(
            IdProvider<Method, String> methodIdProvider) {
        configuration.methodIdProvider = methodIdProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> classPathPatternsProvider(
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider) {
        configuration.classPathPatternsProvider = classPathPatternsProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> methodPathPatternsProvider(
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        configuration.methodPathPatternsProvider = methodPathPatternsProvider;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> rateLimiterFactory(
            RateLimiterFactory<Object> rateLimiterFactory) {
        configuration.rateLimiterFactory = rateLimiterFactory;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> annotationProcessor(
            AnnotationProcessor<Class<?>, RateLimitConfig> annotationProcessor) {
        configuration.annotationProcessor = annotationProcessor;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> resourceAnnotationTypes(
            Class<? extends Annotation>[] resourceAnnotationTypes) {
        configuration.resourceAnnotationTypes = resourceAnnotationTypes;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> nodeFactoryForProperties(
            NodeBuilder<RateLimitProperties, RateLimitConfig> nodeBuilderForProperties) {
        configuration.nodeBuilderForProperties = nodeBuilderForProperties;
        return this;
    }

    @Override public WebRequestRateLimiterConfig.Builder<REQUEST> nodeFactoryForAnnotations(
            NodeBuilder<List<Class<?>>, RateLimitConfig> nodeBuilderForAnnotations) {
        configuration.nodeBuilderForAnnotations = nodeBuilderForAnnotations;
        return this;
    }
}
