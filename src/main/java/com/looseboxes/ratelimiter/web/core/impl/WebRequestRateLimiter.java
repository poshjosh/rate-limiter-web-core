package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.MatcherFactory;
import com.looseboxes.ratelimiter.web.core.WebRequestRateLimiterConfig;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebRequestRateLimiter<R> implements RateLimiter<R>{

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

    private final RateLimiter<R> compositeRateLimiter;

    public WebRequestRateLimiter(WebRequestRateLimiterConfig<R> webRequestRateLimiterConfig) {

        NodeNamesCollector nodeNamesCollector = new NodeNamesCollector();
        Node<NodeValue<Rates>> propertiesRootNode = webRequestRateLimiterConfig.getNodeBuilderForProperties()
                .buildNode("root.properties", webRequestRateLimiterConfig.getProperties(), nodeNamesCollector);

        UniqueNameEnforcer uniqueNameEnforcer = new UniqueNameEnforcer(nodeNamesCollector.getNodeNames());
        ElementCollector elementCollector = new ElementCollector(
                webRequestRateLimiterConfig.getClassIdProvider(), webRequestRateLimiterConfig.getMethodIdProvider()
        );
        AnnotationProcessor.NodeConsumer<Rates> consumer = uniqueNameEnforcer.andThen(elementCollector);
        Node<NodeValue<Rates>> annotationsRootNode = webRequestRateLimiterConfig.getNodeBuilderForAnnotations()
                .buildNode("root.annotations", webRequestRateLimiterConfig.getResourceClasses(), consumer);

        MatcherFactory<R, Object> annotationsMatcherFactory = new MatcherFactory<R, Object>() {
            @Override
            public Optional<Matcher<R, ?>> createMatcher(String name, Object source) {
                if (source instanceof Class) {
                    return webRequestRateLimiterConfig.getClassMatcherFactory().createMatcher(name, (Class<?>)source);
                }
                if (source instanceof Method) {
                    return webRequestRateLimiterConfig.getMethodMatcherFactory().createMatcher(name, (Method)source);
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

        RateLimiter<R> rateLimiterForProperties = new PatternMatchingRateLimiterFactory<>(
                propertiesRootNode,
                webRequestRateLimiterConfig.getRegistries(),
                propertiesMatcherFactory,
                true
        ).createRateLimiter();


        RateLimiter<R> rateLimiterForAnnotations = new PatternMatchingRateLimiterFactory<>(
                annotationsRootNode,
                webRequestRateLimiterConfig.getRegistries(),
                annotationsMatcherFactory,
                false
        ).createRateLimiter();

        this.compositeRateLimiter = rateLimiterForProperties.andThen(rateLimiterForAnnotations);
    }

    @Override
    public boolean tryConsume(Object context, R resourceId, int permits, long timeout, TimeUnit unit) {
        return compositeRateLimiter.tryConsume(context, resourceId, permits, timeout, unit);
    }
}
