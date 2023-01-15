package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiterComposition;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

final class DefaultResourceLimiterRegistry<R> implements ResourceLimiterRegistry<R> {

    private static class RateConfigCollector implements AnnotationProcessor.NodeConsumer {
        private final Map<String, RateConfig> nameToRateMap;
        public RateConfigCollector() {
            this.nameToRateMap = new HashMap<>();
        }
        @Override
        public void accept(Object genericDeclaration, Node<RateConfig> node) {
            node.getValueOptional().ifPresent(rateConfig -> {
                nameToRateMap.putIfAbsent(node.getName(), rateConfig);
            });
        }
        public Optional<RateConfig> get(String name) {
            return Optional.ofNullable(nameToRateMap.get(name));
        }
    }

    private final RateLimitProperties properties;
    private final Registries<R> registries;
    private final Node<RateConfig> propertiesRootNode;
    private final Node<RateConfig> annotationsRootNode;

    DefaultResourceLimiterRegistry(ResourceLimiterConfig<R> resourceLimiterConfig) {
        properties = resourceLimiterConfig.getProperties();

        registries = Registries.ofDefaults();

        resourceLimiterConfig.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = NodeBuilder.ofProperties()
                .buildNode("root.properties", properties, propertyConfigs);

        Node<RateConfig> annoRoot = NodeBuilder.ofClasses(resourceLimiterConfig.getAnnotationProcessor())
                .buildNode("root.annotations", resourceLimiterConfig.getResourceClasses());

        List<String> transferredToAnnotations = new ArrayList<>();
        Function<Node<RateConfig>, RateConfig> overrideWithPropertyValue = node -> {
            if (node.isRoot()) {
                return node.getValueOrDefault(null);
            }
            RateConfig annotationConfig = Checks.requireNodeValue(node);
            return propertyConfigs.get(node.getName())
                    .map(propertyConfig -> {
                        transferredToAnnotations.add(node.getName());
                        return propertyConfig.withSource(annotationConfig.getSource());
                    }).orElse(annotationConfig);
        };

        annotationsRootNode = annoRoot.transform(overrideWithPropertyValue);

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Node.of("root.properties"));

        register(resourceLimiterConfig, propertiesRootNode, annotationsRootNode);
    }

    private void register(
            ResourceLimiterConfig<R> resourceLimiterConfig,
            Node<RateConfig> propertiesRootNode,
            Node<RateConfig> annotationsRootNode) {

        new RegistrationHandler<>(
                registries,
                resourceLimiterConfig.getMatcherFactory(),
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(annotationsRootNode);

        new RegistrationHandler<>(
                registries,
                resourceLimiterConfig.getMatcherFactory(),
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(propertiesRootNode);
    }

    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent();
    }

    private boolean isRateLimited(String id, Node<RateConfig> node) {
        return id.equals(node.getName());
    }

    public ResourceLimiter<R> createResourceLimiter() {

        ResourceLimiterComposition.MatcherProvider<R> matcherProvider = node -> {
            if (isRateLimitingEnabled()) {
                return registries.matchers().getOrDefault(node.getName());
            }
            return Matcher.matchNone();
        };

        ResourceLimiterComposition.LimiterProvider limiterProvider = node -> {
            if (isRateLimitingEnabled()) {
                return registries.limiters().getOrDefault(node.getName());
            }
            return ResourceLimiter.noop();
        };

        ResourceLimiter<R> limiterForProperties = ResourceLimiterComposition.ofProperties(
                matcherProvider, limiterProvider, propertiesRootNode
        );

        ResourceLimiter<R> limiterForAnnotations = ResourceLimiterComposition.ofAnnotations(
                matcherProvider, limiterProvider, annotationsRootNode
        );

        return limiterForProperties.andThen(limiterForAnnotations);
    }

    public RateLimitProperties properties() {
        return properties;
    }

    @Override public UnmodifiableRegistry<ResourceLimiter<?>> limiters() {
        return Registry.unmodifiable(registries.limiters());
    }

    @Override public UnmodifiableRegistry<Matcher<R, ?>> matchers() {
        return Registry.unmodifiable(registries.matchers());
    }

    @Override public UnmodifiableRegistry<RateCache<?>> caches() {
        return Registry.unmodifiable(registries.caches());
    }

    @Override public UnmodifiableRegistry<UsageListener> listeners() {
        return Registry.unmodifiable(registries.listeners());
    }
}
