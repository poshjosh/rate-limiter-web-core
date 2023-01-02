package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.PatternMatchingResourceLimiter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.Element;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;

import java.util.*;

public abstract class AbstractResourceLimiterRegistry<R> {

    private static class NodeNamesCollector implements AnnotationProcessor.NodeConsumer<Rates> {
        private Set<String> nodeNames;
        @Override public void accept(Object source, Node<NodeValue<Rates>> node) {
            if (nodeNames == null) {
                nodeNames = new HashSet<>();
            }
            nodeNames.add(node.getName());
        }
        public Set<String> getNodeNames() {
            return nodeNames == null ? Collections.emptySet() : Collections.unmodifiableSet(nodeNames);
        }
    }

    private static class UniqueNameEnforcer implements AnnotationProcessor.NodeConsumer<Rates> {
        private final Set<String> alreadyUsedNodeName;
        public UniqueNameEnforcer(Set<String> alreadyUsedNodeName) {
            this.alreadyUsedNodeName = Objects.requireNonNull(alreadyUsedNodeName);
        }
        @Override public void accept(Object source, Node<NodeValue<Rates>> node) {
            if(node != null && alreadyUsedNodeName.contains(node.getName())) {
                throw new IllegalStateException("Node name: " + node.getName() + ", already used at: " + source);
            }
        }
    }

    private static class ElementCollector implements AnnotationProcessor.NodeConsumer<Rates> {
        private final Map<String, Element> nameToElementMap;
        public ElementCollector() {
            this.nameToElementMap = new HashMap<>();
        }
        @Override
        public void accept(Object source, Node<NodeValue<Rates>> node) {
            if (source instanceof Element) {
                Element element = (Element)source;
                nameToElementMap.putIfAbsent(element.getId(), element);
            }
        }
        public Element getElement(String name) {
            return nameToElementMap.get(name);
        }
    }

    private static final class Registration<R> {
        private final Node<NodeValue<Rates>> propertiesRootNode;
        private final Node<NodeValue<Rates>> annotationsRootNode;
        private Registration(
                Node<NodeValue<Rates>> propertiesRootNode,
                Node<NodeValue<Rates>> annotationsRootNode) {
            this.propertiesRootNode = Objects.requireNonNull(propertiesRootNode);
            this.annotationsRootNode = Objects.requireNonNull(annotationsRootNode);
        }
    }

    private final WebResourceLimiterConfig<R> webResourceLimiterConfig;

    protected AbstractResourceLimiterRegistry(WebResourceLimiterConfig<R> webResourceLimiterConfig) {
        this.webResourceLimiterConfig = Objects.requireNonNull(webResourceLimiterConfig);
    }

    public ResourceLimiter<R> createResourceLimiter() {

        Registration<R> registration = register();

        ResourceLimiter<R> resourceLimiterForProperties = createRateLimiter(
                webResourceLimiterConfig.getRegistries(), registration.propertiesRootNode, true
        );

        ResourceLimiter<R> resourceLimiterForAnnotations = createRateLimiter(
                webResourceLimiterConfig.getRegistries(), registration.annotationsRootNode, false
        );

        return resourceLimiterForProperties.andThen(resourceLimiterForAnnotations);
    }

    public Registries<R> init() {
        register();
        return webResourceLimiterConfig.getRegistries();
    }

    private Registration<R> register() {
        NodeNamesCollector nodeNamesCollector = new NodeNamesCollector();
        Node<NodeValue<Rates>> propertiesRootNode = webResourceLimiterConfig.getNodeBuilderForProperties()
                .buildNode("root.properties", webResourceLimiterConfig.getProperties(), nodeNamesCollector);

        UniqueNameEnforcer uniqueNameEnforcer = new UniqueNameEnforcer(nodeNamesCollector.getNodeNames());
        ElementCollector elementCollector = new ElementCollector();
        AnnotationProcessor.NodeConsumer<Rates> consumer = uniqueNameEnforcer.andThen(elementCollector);
        Node<NodeValue<Rates>> annotationsRootNode = webResourceLimiterConfig.getNodeBuilderForAnnotations()
                .buildNode("root.annotations", webResourceLimiterConfig.getResourceClasses(), consumer);
        register(propertiesRootNode, annotationsRootNode, elementCollector);
        return new Registration<>(propertiesRootNode, annotationsRootNode);
    }

    private void register(Node<NodeValue<Rates>> propertiesRootNode,
                               Node<NodeValue<Rates>> annotationsRootNode,
                               ElementCollector elementCollector) {

        MatcherFactory<R, Object> propertiesMatcherFactory = new MatcherFactory<R, Object>() {
            @Override
            public Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
                // If the name matches an element source (e.g class/method) name,
                // then use the MatcherFactory for that element
                // This means that if the a property has a name that matches
                Element elementSource = elementCollector.getElement(name);
                if (elementSource != null) {
                    return webResourceLimiterConfig.getMatcherFactory().createMatcher(name, elementSource);
                }
                return Optional.empty();
            }
        };

        new InternalRegistry<>(
                webResourceLimiterConfig.getRegistries(),
                propertiesMatcherFactory
        ).registerMatchersAndRateLimiters(propertiesRootNode);

        new InternalRegistry<>(
                webResourceLimiterConfig.getRegistries(),
                webResourceLimiterConfig.getMatcherFactory()
        ).registerMatchersAndRateLimiters(annotationsRootNode);
    }

    private ResourceLimiter<R> createRateLimiter(
            Registries<R> registries, Node<NodeValue<Rates>> rootNode, boolean firstMatchOnly) {

        PatternMatchingResourceLimiter.MatcherProvider<Rates, R> matcherProvider =
                node -> registries.matchers().getOrDefault(node.getName());

        PatternMatchingResourceLimiter.LimiterProvider<Rates> limiterProvider =
                node -> registries.limiters().getOrDefault(node.getName());

        return new PatternMatchingResourceLimiter<>(
                matcherProvider, limiterProvider, rootNode, firstMatchOnly);
    }
}
