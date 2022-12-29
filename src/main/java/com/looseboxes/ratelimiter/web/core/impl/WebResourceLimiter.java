package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.WebResourceLimiterConfig;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebResourceLimiter<R> implements ResourceLimiter<R> {

    private static class NodeNamesCollector implements AnnotationProcessor.NodeConsumer<Rates> {
        private Set<String> nodeNames;
        @Override public void accept(Object o, Node<NodeValue<Rates>> node) {
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
        private final IdProvider<Class<?>, String> classIdProvider;
        private final IdProvider<Method, String> methodIdProvider;
        private final Map<String, Object> nameToElementMap;
        public ElementCollector(IdProvider<Class<?>, String> classIdProvider, IdProvider<Method, String> methodIdProvider) {
            this.classIdProvider = Objects.requireNonNull(classIdProvider);
            this.methodIdProvider = Objects.requireNonNull(methodIdProvider);
            this.nameToElementMap = new HashMap<>();
        }
        @Override
        public void accept(Object o, Node<NodeValue<Rates>> node) {
            if (o instanceof Class) {
                nameToElementMap.putIfAbsent(classIdProvider.getId((Class)o), o);
            } else if (o instanceof Method) {
                nameToElementMap.putIfAbsent(methodIdProvider.getId((Method)o), o);
            }
        }
        public Object getElement(String name) {
            return nameToElementMap.get(name);
        }
    }

    private final ResourceLimiter<R> compositeResourceLimiter;

    public WebResourceLimiter(WebResourceLimiterConfig<R> webResourceLimiterConfig) {

        NodeNamesCollector nodeNamesCollector = new NodeNamesCollector();
        Node<NodeValue<Rates>> propertiesRootNode = webResourceLimiterConfig.getNodeBuilderForProperties()
                .buildNode("root.properties", webResourceLimiterConfig.getProperties(), nodeNamesCollector);

        UniqueNameEnforcer uniqueNameEnforcer = new UniqueNameEnforcer(nodeNamesCollector.getNodeNames());
        ElementCollector elementCollector = new ElementCollector(
                webResourceLimiterConfig.getClassIdProvider(), webResourceLimiterConfig.getMethodIdProvider()
        );
        AnnotationProcessor.NodeConsumer<Rates> consumer = uniqueNameEnforcer.andThen(elementCollector);
        Node<NodeValue<Rates>> annotationsRootNode = webResourceLimiterConfig.getNodeBuilderForAnnotations()
                .buildNode("root.annotations", webResourceLimiterConfig.getResourceClasses(), consumer);

        MatcherFactory<R, Object> annotationsMatcherFactory = new MatcherFactory<R, Object>() {
            @Override
            public Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
                if (source instanceof Class) {
                    return webResourceLimiterConfig.getClassMatcherFactory().createMatcher(name, (Class<?>)source);
                }
                if (source instanceof Method) {
                    return webResourceLimiterConfig.getMethodMatcherFactory().createMatcher(name, (Method)source);
                }
                return Optional.empty();
            }
        };

        MatcherFactory<R, Object> propertiesMatcherFactory = new MatcherFactory<R, Object>() {
            @Override
            public Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
                // If the name matches an element source (e.g class/method) name,
                // then use the MatcherFactory for that element
                // This means that if the a property has a name that matches
                Object elementSource = elementCollector.getElement(name);
                if (elementSource != null) {
                    return annotationsMatcherFactory.createMatcher(name, elementSource);
                }
                return Optional.empty();
            }
        };

        ResourceLimiter<R> resourceLimiterForProperties = new PatternMatchingResourceLimiterFactory<>(
                propertiesRootNode,
                webResourceLimiterConfig.getRegistries(),
                propertiesMatcherFactory,
                true
        ).createRateLimiter();


        ResourceLimiter<R> resourceLimiterForAnnotations = new PatternMatchingResourceLimiterFactory<>(
                annotationsRootNode,
                webResourceLimiterConfig.getRegistries(),
                annotationsMatcherFactory,
                false
        ).createRateLimiter();

        this.compositeResourceLimiter = resourceLimiterForProperties.andThen(resourceLimiterForAnnotations);
    }

    @Override
    public boolean tryConsume(Object context, R resourceId, int permits, long timeout, TimeUnit unit) {
        return compositeResourceLimiter.tryConsume(context, resourceId, permits, timeout, unit);
    }
}
