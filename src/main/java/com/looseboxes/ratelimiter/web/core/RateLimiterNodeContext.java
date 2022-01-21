package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.BreadthFirstNodeVisitor;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RateLimiterNodeContext<R, K>{

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterNodeContext.class);

    private final Node<NodeData<RateLimiter<K>>> propertiesRoot;
    private final Node<NodeData<RateLimiter<K>>> annotationsRoot;

    public RateLimiterNodeContext(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        // Create rate limiters for specified properties
        //
        final Node<NodeData<RateConfigList>> propertyRoot =
                new NodeFromPropertiesFactory().createNode("root.properties", properties);

        this.propertiesRoot = toRateLimiterNode(rateLimiterConfigurationSource, propertyRoot);

        // Create rate limiters for annotated classes/methods
        //
        Node<NodeData<RateConfigList>> elementRoot =
                new NodeFromAnnotationsFactory(annotationProcessor).createNode("root.annotations", resourceClasses);

        requireSameNameNotUsedForBothPropertyAndAnnotationBasedGroups(propertyRoot, elementRoot);

        this.annotationsRoot = toRateLimiterNode(rateLimiterConfigurationSource, elementRoot);
    }

    private void requireSameNameNotUsedForBothPropertyAndAnnotationBasedGroups(
            Node<NodeData<RateConfigList>> propertyRoot,
            Node<NodeData<RateConfigList>> elementRoot) {

        final Set<String> propertyGroupNames = collectNodes((node) -> true, propertyRoot).stream()
                .map(Node::getName).collect(Collectors.toSet());

        new BreadthFirstNodeVisitor<NodeData<RateConfigList>>(node -> {
            if(node != null && propertyGroupNames.contains(node.getName())) {
                throw new IllegalStateException(
                        "The same name cannot be used for both property based and annotation based rate limit group. Name: " + node.getName());
            }
        }).accept(elementRoot);
    }

    private Node<NodeData<RateLimiter<K>>> toRateLimiterNode(
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            Node<NodeData<RateConfigList>> source) {

        BiPredicate<String, NodeData<RateConfigList>> notRoot =
                (nodeName, nodeData) -> !NodeUtil.isEqual(source, nodeName, nodeData);

        // Transform the root and it's children to rate limiter nodes
        final Node<NodeData<RateLimiter<K>>> rateLimiterRootNode = source
                .transform(null, new NodeValueConverterImpl<>(notRoot, rateLimiterConfigurationSource));

        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));
        }

        return rateLimiterRootNode;
    }

    public List<Node<NodeData<RateLimiter<K>>>> getPropertyNodes(Predicate<Node<NodeData<RateLimiter<K>>>> filter) {
        return collectNodes(filter, propertiesRoot);
    }

    public Node<NodeData<RateLimiter<K>>> getPropertiesRoot() {
        return propertiesRoot;
    }

    public List<Node<NodeData<RateLimiter<K>>>> getAnnotationNodes(Predicate<Node<NodeData<RateLimiter<K>>>> filter) {
        return collectNodes(filter, annotationsRoot);
    }

    private <T> List<Node<T>> collectNodes(Predicate<Node<T>> filter, Node<T> root) {
        List<Node<T>> nodes = new ArrayList<>();
        new BreadthFirstNodeVisitor<>(filter, nodes::add).accept(root);
        return nodes;
    }

    public Node<NodeData<RateLimiter<K>>> getAnnotationsRoot() {
        return annotationsRoot;
    }
}
