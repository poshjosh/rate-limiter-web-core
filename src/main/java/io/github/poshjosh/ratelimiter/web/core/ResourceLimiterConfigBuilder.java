package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;

import java.util.*;
import java.util.function.Supplier;

/**
 * @param <REQUEST> The type of the request related object
 */
final class ResourceLimiterConfigBuilder<REQUEST>
        implements ResourceLimiterConfig.Builder<REQUEST> {

    private static final class ResourceLimiterConfigImpl<T> extends ResourceLimiterConfig<T> {

        private RateLimitProperties properties;
        private ResourceLimiterConfigurer<T> configurer;
        private PathPatternsProvider pathPatternsProvider;
        private RequestToIdConverter<T, String> requestToIdConverter;
        private ExpressionMatcher<T, Object> expressionMatcher;
        private ResourceLimiterFactory<Object> resourceLimiterFactory;
        private ClassesInPackageFinder classesInPackageFinder;
        private AnnotationProcessor<Class<?>> annotationProcessor;

        private Supplier<List<Class<?>>> resourceClassesSupplier;

        private MatcherFactory<T> matcherFactory;

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
        @Override public List<Class<?>> getResourceClasses() { return Collections.emptyList(); }
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

        Objects.requireNonNull(configuration.requestToIdConverter);

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
            annotationProcessor(AnnotationProcessor.ofDefaults());
        }

        configuration.resourceClassesSupplier = () -> {
            Set<Class<?>> classes = new HashSet<>();
            classes.addAll(configuration.getProperties().getResourceClasses());
            classes.addAll(configuration.classesInPackageFinder
                    .findClasses(configuration.properties.getResourcePackages()));
            return new ArrayList<>(classes);
        };

        if (configuration.expressionMatcher == null) {
            configuration.expressionMatcher = ExpressionMatcher.matchNone();
        }

        configuration.matcherFactory = new DefaultMatcherFactory<>(
                configuration.pathPatternsProvider,
                configuration.requestToIdConverter,
                configuration.expressionMatcher);

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

    @Override public ResourceLimiterConfig.Builder<REQUEST> pathPatternsProvider(
            PathPatternsProvider pathPatternsProvider) {
        configuration.pathPatternsProvider = pathPatternsProvider;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> requestToIdConverter(
            RequestToIdConverter<REQUEST, String> requestToIdConverter) {
        configuration.requestToIdConverter = requestToIdConverter;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> expressionMatcher(
            ExpressionMatcher<REQUEST, Object> expressionMatcher) {
        configuration.expressionMatcher = expressionMatcher;
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
