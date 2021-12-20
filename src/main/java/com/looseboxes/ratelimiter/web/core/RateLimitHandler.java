package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RateLimitHandler<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitHandler.class);

    private final String rootNodeName = "root";
    private final RateLimiter<R> rateLimiterFromProperties;
    private final RateLimiter<R> rateLimiterFromAnnotations;

    public RateLimitHandler(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        final Node<NodeValue<RateLimitConfig>> rootNode = addNodesToRoot(properties.getRateLimitConfigs());
        final Set<String> propertyGroupNames = new LinkedHashSet<>();
        collectLeafNodes(rootNode, node -> propertyGroupNames.add(node.getName()));

        this.rateLimiterFromProperties = createRateLimiter(properties, rateLimiterConfigurationSource, rootNode, false);

        this.rateLimiterFromAnnotations = createRateLimiterFromAnnotations(properties, rateLimiterConfigurationSource, propertyGroupNames, resourceClasses, annotationProcessor);
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new BreadthFirstNodeVisitor<>(Node::isLeaf, collector).accept(root);
    }

    private RateLimiter<R> createRateLimiterFromAnnotations(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            Set<String> propertyGroupNames,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        Node<NodeValue<RateLimitConfig>> rootNode = NodeUtil.createNode(rootNodeName);

        final BiConsumer<Object, Node<NodeValue<RateLimitConfig>>> requirePropertyGroupNameNotEqualToAnnotationGroupName = (element, node) -> {
            if(node != null && propertyGroupNames.contains(node.getName())) {
                throw new IllegalStateException(
                        "The same name cannot be used for both property based and annotation based rate limit group. Name: " + node.getName());
            }
        };

        annotationProcessor.process(rootNode, resourceClasses, requirePropertyGroupNameNotEqualToAnnotationGroupName);

        return createRateLimiter(properties, rateLimiterConfigurationSource, rootNode, true);
    }

    private RateLimiter<R> createRateLimiter(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            Node<NodeValue<RateLimitConfig>> rootNode,
            boolean firstMatchOnly) {

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeValue<RateLimiter<Object>>> rateLimiterRootNode = rootNode
                .transform(null, (name, value) -> name, new NodeValueConverter(rootNode, rateLimiterConfigurationSource));
        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        Predicate<R> filter = request -> !Boolean.TRUE.equals(properties.getDisabled());

        return new PatternMatchingRateLimiter<>(filter, rateLimiterConfigurationSource, rateLimiterRootNode, firstMatchOnly);
    }

    public void handleRequest(R request) {
        try {
            this.rateLimiterFromProperties.increment(request);
        }finally {
            this.rateLimiterFromAnnotations.increment(request);
        }
    }

    private Node<NodeValue<RateLimitConfig>> addNodesToRoot(Map<String, RateLimitConfig> rateLimitConfigs) {
        Map<String, RateLimitConfig> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        RateLimitConfig rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeValue<RateLimitConfig> nodeValue = rootNodeConfig == null ? null : new NodeValue<>(null, rootNodeConfig);
        Node<NodeValue<RateLimitConfig>> rootNode = NodeUtil.createNode(rootNodeName, nodeValue, null);
        NodeUtil.createNodes(rootNode, configsWithoutParent);
        return rootNode;
    }
}
