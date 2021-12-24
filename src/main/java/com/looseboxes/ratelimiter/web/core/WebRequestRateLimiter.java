package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebRequestRateLimiter<R> implements RateLimiter<R>{

    private static final Logger LOG = LoggerFactory.getLogger(WebRequestRateLimiter.class);

    private final String rootNodeName = "root";
    private final RateLimiter<R> rateLimiterFromProperties;
    private final RateLimiter<R> rateLimiterFromAnnotations;

    public WebRequestRateLimiter(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        final Node<NodeValue<RateConfigList>> rootNode = addNodesToRoot(properties.getRateLimitConfigs());
        final Set<String> propertyGroupNames = new LinkedHashSet<>();
        collectLeafNodes(rootNode, node -> propertyGroupNames.add(node.getName()));

        this.rateLimiterFromProperties = createRateLimiter(properties, rateLimiterConfigurationSource, rootNode, false);

        this.rateLimiterFromAnnotations = createRateLimiterFromAnnotations(properties, rateLimiterConfigurationSource, propertyGroupNames, resourceClasses, annotationProcessor);
    }

    @Override
    public boolean increment(R request, int amount) {
        int failCount = 0;
        try {
            if(!this.rateLimiterFromProperties.increment(request, amount)) {
                ++failCount;
            }
        }finally {
            if(!this.rateLimiterFromAnnotations.increment(request, amount)) {
                ++failCount;
            }
        }
        return failCount == 0;
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

        Node<NodeValue<RateConfigList>> rootNode = NodeUtil.createNode(rootNodeName);

        final BiConsumer<Object, Node<NodeValue<RateConfigList>>> requirePropertyGroupNameNotEqualToAnnotationGroupName = (element, node) -> {
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
            Node<NodeValue<RateConfigList>> rootNode,
            boolean firstMatchOnly) {

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeValue<RateLimiter<?>>> rateLimiterRootNode = rootNode
                .transform(null, (name, value) -> name, new NodeValueConverter(rootNode, rateLimiterConfigurationSource));
        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        Predicate<R> filter = request -> !Boolean.TRUE.equals(properties.getDisabled());

        return new PatternMatchingRateLimiter<>(filter, rateLimiterConfigurationSource, rateLimiterRootNode, firstMatchOnly);
    }

    private Node<NodeValue<RateConfigList>> addNodesToRoot(Map<String, RateConfigList> rateLimitConfigs) {
        Map<String, RateConfigList> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        RateConfigList rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeValue<RateConfigList> nodeValue = rootNodeConfig == null ? null : new NodeValue<>(null, rootNodeConfig);
        Node<NodeValue<RateConfigList>> rootNode = NodeUtil.createNode(rootNodeName, nodeValue, null);
        NodeUtil.createNodes(rootNode, configsWithoutParent);
        return rootNode;
    }
}
