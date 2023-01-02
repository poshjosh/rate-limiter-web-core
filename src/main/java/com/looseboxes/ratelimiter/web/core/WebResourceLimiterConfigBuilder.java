package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.util.PathPatternsProvider;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * @param <REQUEST> The type of the request related object
 */
final class WebResourceLimiterConfigBuilder<REQUEST>
        implements WebResourceLimiterConfig.Builder<REQUEST> {

    private static class WebResourceLimiterConfigImpl<T> extends WebResourceLimiterConfig<T> {

        private RateLimitProperties properties;
        private ResourceLimiterConfigurer<T> configurer;
        private RequestToIdConverter<T, String> requestToIdConverter;
        private PathPatternsProvider pathPatternsProvider;
        private MatcherFactory<T, Element> matcherFactory;
        private ResourceLimiterFactory<Object> resourceLimiterFactory;
        private ClassesInPackageFinder classesInPackageFinder;
        private AnnotationProcessor<Class<?>, Rates> annotationProcessor;
        private Class<? extends Annotation> [] resourceAnnotationTypes;

        private Registries<T> registries;
        private ResourceClassesSupplier resourceClassesSupplier;

        private NodeBuilder<RateLimitProperties, Rates> nodeBuilderForProperties;
        private NodeBuilder<List<Class<?>>, Rates> nodeBuilderForAnnotations;

        @Override public RateLimitProperties getProperties() {
            return properties;
        }

        @Override public Registries<T> getRegistries() {
            return registries;
        }

        // Package access getters
        //
        @Override MatcherFactory<T, Element> getMatcherFactory() { return matcherFactory; }

        @Override ResourceClassesSupplier getResourceClassesSupplier() {
            return resourceClassesSupplier;
        }

        @Override NodeBuilder<RateLimitProperties, Rates> getNodeBuilderForProperties() {
            return nodeBuilderForProperties;
        }

        @Override NodeBuilder<List<Class<?>>, Rates> getNodeBuilderForAnnotations() {
            return nodeBuilderForAnnotations;
        }

        @Override ResourceLimiterFactory<Object> getResourceLimiterFactory() {
            return resourceLimiterFactory;
        }
    }

    private final WebResourceLimiterConfigImpl<REQUEST> configuration;

    WebResourceLimiterConfigBuilder() {
        this.configuration = new WebResourceLimiterConfigImpl<>();
    }

    @Override public WebResourceLimiterConfig<REQUEST> build() {
        if (configuration.properties == null) {
            configuration.properties = new DefaultRateLimitProperties();
        }
        if (configuration.resourceAnnotationTypes.length == 0) {
            throw new IndexOutOfBoundsException("Index: 0");
        }
        if (configuration.resourceLimiterFactory == null) {
            resourceLimiterFactory(ResourceLimiterFactory.ofDefaults());
        }
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(ClassesInPackageFinder.ofDefaults());
        }
        if (configuration.annotationProcessor == null) {
            annotationProcessor(AnnotationProcessor.ofRates());
        }
        if (configuration.nodeBuilderForProperties == null) {
            nodeFactoryForProperties(new PropertiesToRatesNodeBuilder());
        }
        if (configuration.nodeBuilderForAnnotations == null) {
            nodeFactoryForAnnotations(new ClassesToRatesNodeBuilder(configuration.annotationProcessor));
        }

        if (configuration.matcherFactory == null) {
            matcherFactory(new DefaultMatcherFactory<>(
                    configuration.pathPatternsProvider, configuration.requestToIdConverter));
        }

        configuration.registries = new DefaultRegistries<>(ResourceLimiter.noop(), Matcher.matchNone());

        configuration.resourceClassesSupplier = new DefaultResourceClassesSupplier(
                configuration.classesInPackageFinder,
                configuration.properties.getResourcePackages(),
                configuration.resourceAnnotationTypes);

        if (configuration.configurer != null) {
            configuration.configurer.configure(configuration.registries);
        }

        return configuration;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> properties(
            RateLimitProperties properties) {
        configuration.properties = properties;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> configurer(
            ResourceLimiterConfigurer<REQUEST> configurer) {
        configuration.configurer = configurer;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> requestToIdConverter(
            RequestToIdConverter<REQUEST, String> requestToIdConverter) {
        configuration.requestToIdConverter = requestToIdConverter;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> pathPatternsProvider(
            PathPatternsProvider pathPatternsProvider) {
        configuration.pathPatternsProvider = pathPatternsProvider;
        return this;
    }

    @Override
    public WebResourceLimiterConfig.Builder<REQUEST> matcherFactory(MatcherFactory<REQUEST, Element> matcherFactory) {
        configuration.matcherFactory = matcherFactory;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> resourceLimiterFactory(
            ResourceLimiterFactory<Object> resourceLimiterFactory) {
        configuration.resourceLimiterFactory = resourceLimiterFactory;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> annotationProcessor(
            AnnotationProcessor<Class<?>, Rates> annotationProcessor) {
        configuration.annotationProcessor = annotationProcessor;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> resourceAnnotationTypes(
            Class<? extends Annotation>[] resourceAnnotationTypes) {
        configuration.resourceAnnotationTypes = resourceAnnotationTypes;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> nodeFactoryForProperties(
            NodeBuilder<RateLimitProperties, Rates> nodeBuilderForProperties) {
        configuration.nodeBuilderForProperties = nodeBuilderForProperties;
        return this;
    }

    @Override public WebResourceLimiterConfig.Builder<REQUEST> nodeFactoryForAnnotations(
            NodeBuilder<List<Class<?>>, Rates> nodeBuilderForAnnotations) {
        configuration.nodeBuilderForAnnotations = nodeBuilderForAnnotations;
        return this;
    }
}
