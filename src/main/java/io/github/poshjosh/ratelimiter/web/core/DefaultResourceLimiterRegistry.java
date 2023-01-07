package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.MatchedResourceLimiter;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.cache.RateCache;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import java.util.*;

final class DefaultResourceLimiterRegistry<R> implements ResourceLimiterRegistry<R> {

    private static class NodeNamesCollector implements AnnotationProcessor.NodeConsumer {
        private final Set<String> nodeNames = new HashSet<>();
        @Override public void accept(Object source, Node<RateConfig> node) {
            nodeNames.add(node.getName());
        }
        public Set<String> getNodeNames() {
            return Collections.unmodifiableSet(nodeNames);
        }
    }

    private static class UniqueIdEnforcer implements AnnotationProcessor.NodeConsumer {
        private final Set<String> alreadyUsedNodeName;
        public UniqueIdEnforcer(Set<String> alreadyUsedNodeName) {
            this.alreadyUsedNodeName = Objects.requireNonNull(alreadyUsedNodeName);
        }
        @Override public void accept(Object source, Node<RateConfig> node) {
            if(!node.isEmpty() && alreadyUsedNodeName.contains(node.getName())) {
                throw new IllegalStateException("Node name: " + node.getName() +
                        ", already used at: " + source);
            }
        }
    }

    private static class ElementCollector implements AnnotationProcessor.NodeConsumer {
        private final Map<String, Element> nameToElementMap;
        public ElementCollector() {
            this.nameToElementMap = new HashMap<>();
        }
        @Override
        public void accept(Object source, Node<RateConfig> node) {
            if (source instanceof Element) {
                Element element = (Element)source;
                nameToElementMap.putIfAbsent(element.getId(), element);
            }
        }
        public Optional<Element> get(String name) {
            return Optional.ofNullable(nameToElementMap.get(name));
        }
    }

    private final RateLimitProperties properties;
    private final Registries<R> registries;
    private final Node<RateConfig> propertiesRootNode;
    private final Node<RateConfig> annotationsRootNode;

    DefaultResourceLimiterRegistry(ResourceLimiterConfig<R> resourceLimiterConfig) {
        properties = resourceLimiterConfig.getProperties();

        registries = new DefaultRegistries<>(
                ResourceLimiter.noop(), Matcher.matchNone(),
                RateCache.ofMap(), UsageListener.NO_OP);

        resourceLimiterConfig.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        NodeNamesCollector nodeNamesCollector = new NodeNamesCollector();
        propertiesRootNode = NodeBuilder.ofProperties()
                .buildNode("root.properties", properties, nodeNamesCollector);

        UniqueIdEnforcer uniqueIdEnforcer = new UniqueIdEnforcer(nodeNamesCollector.getNodeNames());
        ElementCollector elementCollector = new ElementCollector();
        AnnotationProcessor.NodeConsumer consumer = uniqueIdEnforcer.andThen(elementCollector);
        annotationsRootNode = NodeBuilder.ofClasses(resourceLimiterConfig.getAnnotationProcessor())
                .buildNode("root.annotations", resourceLimiterConfig.getResourceClasses(), consumer);

        register(resourceLimiterConfig, propertiesRootNode, annotationsRootNode, elementCollector);
    }

    private void register(
            ResourceLimiterConfig<R> resourceLimiterConfig,
            Node<RateConfig> propertiesRootNode,
            Node<RateConfig> annotationsRootNode,
            ElementCollector elementCollector) {

        MatcherFactory<R> propertiesMatcherFactory = (name, rateConfig) -> {
            // If the name matches an element source (e.g class/method) name,
            // then use the MatcherFactory for that element
            // This means that if the a property has a name that matches
            return elementCollector.get(name)
                    .map(element -> RateConfig.of(element, rateConfig.getValue()))
                    .flatMap(config ->
                            resourceLimiterConfig.getMatcherFactory().createMatcher(name, config));
        };

        // Annotation based comes before properties, so they can be overwritten by properties
        //
        new RegistrationHandler<>(
                registries,
                resourceLimiterConfig.getMatcherFactory(),
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(annotationsRootNode);

        new RegistrationHandler<>(
                registries,
                propertiesMatcherFactory,
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(propertiesRootNode);
    }

    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> node.getName().equals(id)).isPresent()
                || annotationsRootNode.findFirstChild(node -> node.getName().equals(id)).isPresent();
    }

    public ResourceLimiter<R> createResourceLimiter() {

        MatchedResourceLimiter.MatcherProvider<R> matcherProvider = node -> {
            if (isRateLimitingEnabled()) {
                return registries.matchers().getOrDefault(node.getName());
            }
            return Matcher.matchNone();
        };

        MatchedResourceLimiter.LimiterProvider limiterProvider = node -> {
            if (isRateLimitingEnabled()) {
                return registries.limiters().getOrDefault(node.getName());
            }
            return ResourceLimiter.noop();
        };

        ResourceLimiter<R> limiterForProperties = MatchedResourceLimiter.ofProperties(
                matcherProvider, limiterProvider, propertiesRootNode
        );

        ResourceLimiter<R> limiterForAnnotations = MatchedResourceLimiter.ofAnnotations(
                matcherProvider, limiterProvider, annotationsRootNode
        );

        return limiterForProperties.andThen(limiterForAnnotations);
    }

    public RateLimitProperties properties() {
        return properties;
    }

    @Override public Registry<ResourceLimiter<?>> limiters() {
        return registries.limiters();
    }

    @Override public Registry<Matcher<R, ?>> matchers() {
        return registries.matchers();
    }

    @Override public Registry<RateCache<?>> caches() {
        return registries.caches();
    }

    @Override public Registry<UsageListener> listeners() {
        return registries.listeners();
    }
}
