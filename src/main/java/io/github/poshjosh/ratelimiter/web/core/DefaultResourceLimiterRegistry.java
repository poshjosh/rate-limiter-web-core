package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

final class DefaultResourceLimiterRegistry<R> implements ResourceLimiterRegistry<R> {

    private static class RateConfigCollector implements RateProcessor.NodeConsumer {
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
        Node<RateConfig> propRoot = resourceLimiterConfig.getPropertyRateProcessor()
                .process(Node.of("root.properties"), propertyConfigs, properties);

        Node<RateConfig> annoRoot = resourceLimiterConfig.getClassRateProcessor()
                .processAll(Node.of("root.annotations"), (src, node) -> {}, resourceLimiterConfig.getResourceClasses());

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

        annoRoot = annoRoot.transform(overrideWithPropertyValue);

        Predicate<Node<RateConfig>> isNodeRateLimited = node -> {
            if (node.isRoot()) {
                return true;
            }
            return transferredToAnnotations.contains(node.getName()) || node.getValueOptional()
                    .map(val -> (Element)val.getSource())
                    .filter(Element::isRateLimited).isPresent();
        };

        Predicate<Node<RateConfig>> anyNodeInTreeIsRateLimited = node -> {
            return testTree(node, isNodeRateLimited);
        };

        annotationsRootNode = annoRoot.retainAll(anyNodeInTreeIsRateLimited)
                .orElseGet(() -> Node.of("root.annotations"));

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Node.of("root.properties"));

        register(resourceLimiterConfig, propertiesRootNode, annotationsRootNode);
    }

    private boolean testTree(Node<RateConfig> node, Predicate<Node<RateConfig>> test) {
        return test.test(node) || node.getChildren().stream().anyMatch(child -> testTree(child, test));
    }

    private void register(
            ResourceLimiterConfig<R> resourceLimiterConfig,
            Node<RateConfig> propertiesRootNode,
            Node<RateConfig> annotationsRootNode) {

        new RegistrationHandler<>(registries, resourceLimiterConfig.getMatcherFactory())
                .registerMatchersAndRateLimiters(annotationsRootNode);

        new RegistrationHandler<>(registries, resourceLimiterConfig.getMatcherFactory())
                .registerMatchersAndRateLimiters(propertiesRootNode);
    }

    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent();
    }

    private boolean isRateLimited(String id, Node<RateConfig> node) {
        return id.equals(node.getName());
    }

    public ResourceLimiter<R> createResourceLimiter() {

        MatcherProvider<R> matcherProvider = (nodeName, rateConfig) -> {
            if (isRateLimitingEnabled()) {
                return registries.matchers().getOrDefault(nodeName);
            }
            return Matcher.matchNone();
        };

        ResourceLimiter<R> limiterForProperties = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                registries.getStoreOrDefault(),
                matcherProvider, propertiesRootNode
        );

        ResourceLimiter<R> limiterForAnnotations = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                registries.getStoreOrDefault(),
                matcherProvider, annotationsRootNode
        );

        return limiterForProperties.andThen(limiterForAnnotations);
    }

    public RateLimitProperties properties() {
        return properties;
    }

    @Override public UnmodifiableRegistry<Matcher<R, ?>> matchers() {
        return Registry.unmodifiable(registries.matchers());
    }

    public Optional<BandwidthsStore<?>> getStore() {
        return registries.getStore();
    }

    public Optional<UsageListener> getListener() {
        return registries.getListener();
    }
}
