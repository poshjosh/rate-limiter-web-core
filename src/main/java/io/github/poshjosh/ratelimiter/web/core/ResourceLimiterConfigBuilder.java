package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.annotation.RequestRateAnnotationConverter;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @param <REQUEST> The type of the request related object
 */
final class ResourceLimiterConfigBuilder<REQUEST>
        implements ResourceLimiterConfig.Builder<REQUEST> {

    private static final class ResourceLimiterConfigImpl<T> extends ResourceLimiterConfig<T> {

        private RateLimitProperties properties;
        private ResourceLimiterConfigurer<T> configurer;
        private RequestMatcherFactory<T> requestMatcherFactory;
        private PathPatternsProvider pathPatternsProvider;
        private MatcherFactory<T> matcherFactory;
        private ResourceLimiterFactory<Object> resourceLimiterFactory;
        private ClassesInPackageFinder classesInPackageFinder;
        private AnnotationProcessor<Class<?>> annotationProcessor;

        private Supplier<List<Class<?>>> resourceClassesSupplier;

        // Package access getters
        //
        @Override RateLimitProperties getProperties() {
            return properties;
        }

        @Override Optional<ResourceLimiterConfigurer<T>> getConfigurer() {
            return Optional.ofNullable(configurer);
        }

        @Override Supplier<List<Class<?>>> getResourceClassesSupplier() {
            return resourceClassesSupplier;
        }

        @Override MatcherFactory<T> getMatcherFactory() { return matcherFactory; }

        @Override ResourceLimiterFactory<Object> getResourceLimiterFactory() {
            return resourceLimiterFactory;
        }

        @Override AnnotationProcessor<Class<?>> getAnnotationProcessor() {
            return annotationProcessor;
        }
    }

    private static final class DefaultRateLimitProperties implements RateLimitProperties {
        private DefaultRateLimitProperties() { }
        @Override public List<String> getResourcePackages() {
            return Collections.emptyList();
        }
        @Override public Map<String, Rates> getRateLimitConfigs() {
            return Collections.emptyMap();
        }
    }

    private final ResourceLimiterConfigImpl<REQUEST> configuration;

    ResourceLimiterConfigBuilder() {
        this.configuration = new ResourceLimiterConfigImpl<>();
    }

    @Override public ResourceLimiterConfig<REQUEST> build() {
        if (configuration.properties == null) {
            configuration.properties = new DefaultRateLimitProperties();
        }
        if (configuration.resourceLimiterFactory == null) {
            resourceLimiterFactory(ResourceLimiterFactory.ofDefaults());
        }
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(ClassesInPackageFinder.ofDefaults());
        }
        if (configuration.annotationProcessor == null) {
            annotationProcessor(AnnotationProcessor.of(new RequestRateAnnotationConverter()));
        }

        if (configuration.matcherFactory == null) {
            matcherFactory(new DefaultMatcherFactory<>(
                    configuration.pathPatternsProvider, configuration.requestMatcherFactory));
        }

        configuration.resourceClassesSupplier = () -> configuration.classesInPackageFinder.findClasses(configuration.properties.getResourcePackages());

        return configuration;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> properties(
            RateLimitProperties properties) {
        configuration.properties = properties;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> configurer(
            ResourceLimiterConfigurer<REQUEST> configurer) {
        configuration.configurer = configurer;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> requestMatcherFactory(
            RequestMatcherFactory<REQUEST> requestMatcherFactory) {
        configuration.requestMatcherFactory = requestMatcherFactory;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> pathPatternsProvider(
            PathPatternsProvider pathPatternsProvider) {
        configuration.pathPatternsProvider = pathPatternsProvider;
        return this;
    }

    @Override
    public ResourceLimiterConfig.Builder<REQUEST> matcherFactory(MatcherFactory<REQUEST> matcherFactory) {
        configuration.matcherFactory = matcherFactory;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> resourceLimiterFactory(
            ResourceLimiterFactory<Object> resourceLimiterFactory) {
        configuration.resourceLimiterFactory = resourceLimiterFactory;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> annotationProcessor(
            AnnotationProcessor<Class<?>> annotationProcessor) {
        configuration.annotationProcessor = annotationProcessor;
        return this;
    }
}
