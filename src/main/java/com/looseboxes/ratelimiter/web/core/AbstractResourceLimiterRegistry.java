package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.MatchedResourceLimiter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.UsageListener;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.annotation.ElementId;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.reflect.Method;
import java.util.*;

public abstract class AbstractResourceLimiterRegistry<R> implements Registries<R> {

    private static class NodeNamesCollector implements AnnotationProcessor.NodeConsumer {
        private final Set<String> nodeNames = new HashSet<>();
        @Override public void accept(Object source, Node<RateConfig> node) {
            nodeNames.add(node.getName());
        }
        public Set<String> getNodeNames() {
            return Collections.unmodifiableSet(nodeNames);
        }
    }

    private static class UniqueNameEnforcer implements AnnotationProcessor.NodeConsumer {
        private final Set<String> alreadyUsedNodeName;
        public UniqueNameEnforcer(Set<String> alreadyUsedNodeName) {
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
        public Element getElement(String name) {
            return nameToElementMap.get(name);
        }
    }

    private final RateLimitProperties properties;
    private final Registries<R> registries;
    private final Node<RateConfig> propertiesRootNode;
    private final Node<RateConfig> annotationsRootNode;

    protected AbstractResourceLimiterRegistry(ResourceLimiterConfig<R> resourceLimiterConfig) {
        properties = resourceLimiterConfig.getProperties();

        registries = new DefaultRegistries<>(ResourceLimiter.noop(), Matcher.matchNone());

        resourceLimiterConfig.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        NodeNamesCollector nodeNamesCollector = new NodeNamesCollector();
        propertiesRootNode = NodeBuilder.ofProperties()
                .buildNode("root.properties", properties, nodeNamesCollector);

        UniqueNameEnforcer uniqueNameEnforcer = new UniqueNameEnforcer(nodeNamesCollector.getNodeNames());
        ElementCollector elementCollector = new ElementCollector();
        AnnotationProcessor.NodeConsumer consumer = uniqueNameEnforcer.andThen(elementCollector);
        annotationsRootNode = NodeBuilder.ofClasses(resourceLimiterConfig.getAnnotationProcessor())
                .buildNode("root.annotations", resourceLimiterConfig.getResourceClasses(), consumer);

        register(resourceLimiterConfig, propertiesRootNode, annotationsRootNode, elementCollector);
    }

    private void register(
            ResourceLimiterConfig<R> resourceLimiterConfig,
            Node<RateConfig> propertiesRootNode,
            Node<RateConfig> annotationsRootNode,
            ElementCollector elementCollector) {

        MatcherFactory<R, Object> propertiesMatcherFactory = new MatcherFactory<R, Object>() {
            @Override
            public Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
                // If the name matches an element source (e.g class/method) name,
                // then use the MatcherFactory for that element
                // This means that if the a property has a name that matches
                Element elementSource = elementCollector.getElement(name);
                if (elementSource != null) {
                    return resourceLimiterConfig
                            .getMatcherFactory().createMatcher(name, elementSource);
                }
                return Optional.empty();
            }
        };

        new RegistrationHandler<>(
                registries,
                propertiesMatcherFactory,
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(propertiesRootNode);

        new RegistrationHandler<>(
                registries,
                resourceLimiterConfig.getMatcherFactory(),
                resourceLimiterConfig.getResourceLimiterFactory()
        ).registerMatchersAndRateLimiters(annotationsRootNode);
    }

    public boolean isRateLimited(Class<?> clazz) {
        return isRateLimited(ElementId.of(clazz));
    }

    public boolean isRateLimited(Method method) {
        return isRateLimited(ElementId.of(method));
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

    public boolean isRateLimitingEnabled() {
        final Boolean disabled = properties().getDisabled();
        return disabled == null || Boolean.FALSE.equals(disabled);
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

    @Override public <K> Registry<RateCache<K>> caches() {
        return registries.caches();
    }

    @Override public Registry<UsageListener> listeners() {
        return registries.listeners();
    }
}
