package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
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
        private ClassesInPackageFinder classesInPackageFinder;
        private RateProcessor<Class<?>> classRateProcessor;
        private RateProcessor<RateLimitProperties> propertyRateProcessor;

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

        @Override RateProcessor<Class<?>> getClassRateProcessor() {
            return classRateProcessor;
        }

        @Override public RateProcessor<RateLimitProperties> getPropertyRateProcessor() {
            return propertyRateProcessor;
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
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(ClassesInPackageFinder.ofDefaults());
        }
        if (configuration.classRateProcessor == null) {
            // We accept all class/method  nodes, even those without rate limit related annotations
            // This is because, any of the nodes may have its rate limit related info, specified
            // via properties. Such a node needs to be accepted at this point as property
            // sourced rate limited data will later be transferred to class/method nodes
            classRateProcessor(RateProcessor.of(source -> true));
        }

        if (configuration.propertyRateProcessor == null) {
            propertyRateProcessor(new PropertyRateProcessor());
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

    @Override public ResourceLimiterConfig.Builder<REQUEST> classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> classRateProcessor(
            RateProcessor<Class<?>> rateProcessor) {
        configuration.classRateProcessor = rateProcessor;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder<REQUEST> propertyRateProcessor(
            RateProcessor<RateLimitProperties> rateProcessor) {
        configuration.propertyRateProcessor = rateProcessor;
        return this;
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

    private static final class PropertyRateProcessor implements RateProcessor<RateLimitProperties> {
        private PropertyRateProcessor() { }
        @Override
        public Node<RateConfig> process(
                Node<RateConfig> root, NodeConsumer consumer, RateLimitProperties source) {
            return addNodesToRoot(root, source.getRateLimitConfigs(), consumer);
        }
        private Node<RateConfig> addNodesToRoot(
                Node<RateConfig> rootNode,
                Map<String, Rates> limits,
                NodeConsumer nodeConsumer) {
            Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
            Rates rootNodeConfig = configsWithoutParent.remove(rootNode.getName());
            if (rootNodeConfig != null) {
                throw new IllegalStateException("The name: " + rootNode.getName() +
                        " is reserved, and may not be used to identify rates in " +
                        RateLimitProperties.class.getName());
            }
            nodeConsumer.accept(Rates.empty(), rootNode);
            createNodes(rootNode, configsWithoutParent, nodeConsumer);
            return rootNode;
        }
        private void createNodes(
                Node<RateConfig> parent,
                Map<String, Rates> limits,
                NodeConsumer nodeConsumer) {
            Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
            for (Map.Entry<String, Rates> entry : entrySet) {
                String name = entry.getKey();
                Checks.requireParentNameDoesNotMatchChild(parent.getName(), name);
                Rates rates = entry.getValue();
                Node<RateConfig> node = Node.of(name, RateConfig.of(rates, rates), parent);
                nodeConsumer.accept(rates, node);
            }
        }
    }
}
