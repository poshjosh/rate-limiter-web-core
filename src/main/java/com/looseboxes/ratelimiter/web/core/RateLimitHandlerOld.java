package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.BreadthFirstNodeVisitor;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RateLimitHandlerOld<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitHandlerOld.class);

    private final RateLimitProperties properties;
    private final RateLimiterConfigurationSource<R> rateLimiterConfigurationSource;
    private final Node<NodeValue<RateLimiter<Object>>> rateLimiterRoot;
    private final List<Node<NodeValue<RateLimiter<Object>>>> propertyBasedRateLimiterLeafNodes;
    private final List<Node<NodeValue<RateLimiter<Object>>>> annotationBasedRateLimiterLeafNodes;

    public RateLimitHandlerOld(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        this.properties = Objects.requireNonNull(properties);
        this.rateLimiterConfigurationSource = Objects.requireNonNull(rateLimiterConfigurationSource);

        // First add property based rate limit groups
        final Node<NodeValue<RateLimitConfig>> elementRoot = addNodesToRoot(properties.getRateLimitConfigs());
        final Set<String> propertyGroupNames = new LinkedHashSet<>();
        collectLeafNodes(elementRoot, node -> propertyGroupNames.add(node.getName()));

        final BiConsumer<Object, Node<NodeValue<RateLimitConfig>>> requirePropertyGroupNameNotEqualToAnnotationGroupName = (element, node) -> {
            if(node != null && propertyGroupNames.contains(node.getName())) {
                throw new IllegalStateException(
                        "The same name cannot be used for both property based and annotation based rate limit group. Name: " + node.getName());
            }
        };

        // Add annotation based rate limit groups
        annotationProcessor.process(elementRoot, resourceClasses, requirePropertyGroupNameNotEqualToAnnotationGroupName);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Element Nodes: {}", NodeFormatters.indentedHeirarchy().format(elementRoot));
        }

        // Transform the root and it's children to rate limiter nodes
        this.rateLimiterRoot = elementRoot
                .transform(null, (name, value) -> name, new NodeValueConverter(elementRoot, rateLimiterConfigurationSource));
        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter Nodes: {}", NodeFormatters.indentedHeirarchy().format(this.rateLimiterRoot));
        }

        // Collect property and annotation based leaf nodes separately, because they are accessed differently
        Set<Node<NodeValue<RateLimiter<Object>>>> propertyLeafs = new LinkedHashSet<>();
        Set<Node<NodeValue<RateLimiter<Object>>>> annotationLeafs = new LinkedHashSet<>();
        Consumer<Node<NodeValue<RateLimiter<Object>>>> collector = node -> {
            if(propertyGroupNames.contains(node.getName())) {
                propertyLeafs.add(node);
            }else{
                annotationLeafs.add(node);
            }
        };
        collectLeafNodes(this.rateLimiterRoot, collector);

        this.propertyBasedRateLimiterLeafNodes = Collections.unmodifiableList(new LinkedList<>(propertyLeafs));
        this.annotationBasedRateLimiterLeafNodes = Collections.unmodifiableList(new LinkedList<>(annotationLeafs));
    }

    public void handleRequest(R request) {
        try {
            this.handleRequest(request, 1, this.propertyBasedRateLimiterLeafNodes, false);
        }finally {
            this.handleRequest(request, 1, this.annotationBasedRateLimiterLeafNodes, true);
        }
    }

    public void handleRequest(R request, int amount, List<Node<NodeValue<RateLimiter<Object>>>> rateLimiterNodes, boolean firstMatchOnly) {

        // We check this dynamically, to be able to respond to changes to this property dynamically
        if(Boolean.TRUE.equals(properties.getDisabled())) {
            return;
        }

        for(Node<NodeValue<RateLimiter<Object>>> rateLimiterNode : rateLimiterNodes) {

            Node<NodeValue<RateLimiter<Object>>> currentNode = rateLimiterNode;
            NodeValue<RateLimiter<Object>> nodeValue =  currentNode.getValueOrDefault(null);

            int matchCount = 0;

            while(currentNode != rateLimiterRoot && nodeValue != null) {

                RateLimiter<Object> rateLimiter = nodeValue.getValue();

                if(rateLimiter == RateLimiter.NO_OP) {
                    break;
                }

                Matcher<R> matcher = getOrCreateMatcher(currentNode.getName(), nodeValue);

                final boolean matched = matcher.matches(request);

                if(matched) {
                    rateLimiter.increment(request, amount);
                }else{
                    break;
                }

                ++matchCount;

                currentNode = currentNode.getParentOrDefault(null);
                nodeValue = currentNode == null ? null : currentNode.getValueOrDefault(null);
            }

            if(firstMatchOnly && matchCount > 0) {
                break;
            }
        }
    }

    private Node<NodeValue<RateLimitConfig>> addNodesToRoot(Map<String, RateLimitConfig> rateLimitConfigs) {
        Map<String, RateLimitConfig> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        String rootNodeName = "root";
        RateLimitConfig rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeValue<RateLimitConfig> nodeValue = rootNodeConfig == null ? null : new NodeValue<>(null, rootNodeConfig);
        Node<NodeValue<RateLimitConfig>> rootNode = NodeUtil.createNode(rootNodeName, nodeValue, null);
        NodeUtil.createNodes(rootNode, configsWithoutParent);
        return rootNode;
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new BreadthFirstNodeVisitor<>(Node::isLeaf, collector).accept(root);
    }

    private <V> Matcher<R> getOrCreateMatcher(String name, NodeValue<V> nodeValue){
        return NodeUtil.isPropertyNodeData(nodeValue) ? rateLimiterConfigurationSource.getMatcherForProperties(name) :
                rateLimiterConfigurationSource.getMatcherForSourceElement(name, nodeValue.getSource());
    }
}

