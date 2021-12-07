package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.DefaultRateLimiter;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RateLimitHandler<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitHandler.class);

    private final RateLimitProperties properties;
    private final List<Node<RateLimiter<R>>> propertyBasedRateLimiterLeafNodes;
    private final List<Node<RateLimiter<R>>> annotationBasedRateLimiterLeafNodes;

    public RateLimitHandler(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            List<Class<?>> resourceClasses,
            AnnotationProcessor<Class<?>> annotationProcessor) {

        this.properties = Objects.requireNonNull(properties);

        // First add property based rate limit groups
        final Node<NodeData> elementRoot = addNodesToRoot(properties.getRateLimitConfigs());
        final Set<String> propertyGroupNames = new LinkedHashSet<>();
        collectLeafNodes(elementRoot, node -> propertyGroupNames.add(node.getName()));

        final BiConsumer<Object, Node<NodeData>> requirePropertyGroupNameNotEqualToAnnotationGroupName = (element, node) -> {
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

        BiFunction<String, NodeData, RateLimiter<R>> valueConverter =
                (name, nodeData) -> createRateLimiter(elementRoot, name, nodeData, rateLimiterConfigurationSource);

        // Transform the root and it's children to rate limiter nodes
        Node<RateLimiter<R>> rateLimiterRoot = elementRoot.transform(null, (name, value) -> name, valueConverter);
        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter Nodes: {}", NodeFormatters.indentedHeirarchy().format(rateLimiterRoot));
        }

        // Collect property and annotation based leaf nodes separately, because they are accessed differently
        Set<Node<RateLimiter<R>>> propertyLeafs = new LinkedHashSet<>();
        Set<Node<RateLimiter<R>>> annotationLeafs = new LinkedHashSet<>();
        Consumer<Node<RateLimiter<R>>> collector = node -> {
            if(propertyGroupNames.contains(node.getName())) {
                propertyLeafs.add(node);
            }else{
                annotationLeafs.add(node);
            }
        };
        collectLeafNodes(rateLimiterRoot, collector);

        this.propertyBasedRateLimiterLeafNodes = Collections.unmodifiableList(new LinkedList<>(propertyLeafs));
        this.annotationBasedRateLimiterLeafNodes = Collections.unmodifiableList(new LinkedList<>(annotationLeafs));
    }

    public void handleRequest(R request) {
        try {
            this.handleRequest(request, this.propertyBasedRateLimiterLeafNodes, false);
        }finally {
            this.handleRequest(request, this.annotationBasedRateLimiterLeafNodes, true);
        }
    }

    public void handleRequest(R request, List<Node<RateLimiter<R>>> rateLimiterNodes, boolean firstMatchOnly) {

        // We check this dynamically, to be able to respond to changes to this property dynamically
        if(Boolean.TRUE.equals(properties.getDisabled())) {
            return;
        }

        for(Node<RateLimiter<R>> rateLimiterNode : rateLimiterNodes) {

            Node<RateLimiter<R>> currentNode = rateLimiterNode;
            RateLimiter<R> rateLimiter =  currentNode.getValueOrDefault(null);

            int matchCount = 0;

            while(rateLimiter != null && rateLimiter != RateLimiter.noop()) {

                final Rate rate = rateLimiter.record(request);

                final boolean matched = rate != Rate.NONE;

                if(!matched) {
                    break;
                }

                ++matchCount;

                currentNode = currentNode.getParentOrDefault(null);
                rateLimiter = currentNode == null ? null : currentNode.getValueOrDefault(null);
            }

            if(firstMatchOnly && matchCount > 0) {
                break;
            }
        }
    }

    private Node<NodeData> addNodesToRoot(Map<String, RateLimitConfig> rateLimitConfigs) {
        Map<String, RateLimitConfig> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        String rootNodeName = "root";
        RateLimitConfig rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData nodeData = rootNodeConfig == null ? null : new NodeData(null, rootNodeConfig);
        Node<NodeData> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        NodeUtil.createNodes(rootNode, configsWithoutParent);
        return rootNode;
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new BreadthFirstNodeVisitor<>(Node::isLeaf, collector).accept(root);
    }

    private RateLimiter<R> createRateLimiter(
            Node<NodeData> rootNode,
            String name,
            NodeData nodeData,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource){
        if(isEqual(rootNode, name, nodeData)) {
            return RateLimiter.noop();
        }else {

            RateLimitConfig config = nodeData.getConfig();

            // One method with 3 @RateLimit annotations is a simple group (not really a group)
            // A true group spans either multiple methods/classes
            if(config.getLimits() == null || config.getLimits().isEmpty()) { // This is a group node
                // @TODO how do we handle this?
                // Do we create multiple rate limiters, one for each of the direct children of this group
                // Do we re-use the rate limiters of the children ? They must already exist since we create children first
                return RateLimiter.noop();
            }else {

                RateLimiterConfiguration<Object> rateLimiterConfiguration =
                        rateLimiterConfigurationSource.copyConfigurationOrDefault(name);

                Matcher<R> matcher = getOrCreateMatcher(rootNode, name, nodeData, rateLimiterConfigurationSource);

                return new RequestMatchingRateLimiter<>(matcher, new DefaultRateLimiter<>(rateLimiterConfiguration, config));
            }
        }
    }

    private Matcher<R> getOrCreateMatcher(
            Node<NodeData> rootNode, String name,
            NodeData nodeData, RateLimiterConfigurationSource<R> rateLimiterConfigurationSource){

        return isEqual(rootNode, name, nodeData) ? Matcher.matchNone() :
                NodeUtil.isPropertyNodeData(nodeData) ? rateLimiterConfigurationSource.getMatcherForProperties(name) :
                        rateLimiterConfigurationSource.getMatcherForSourceElement(name, nodeData.getSource());
    }

    private boolean isEqual(Node<NodeData> node, String name, NodeData nodeData) {
        return Objects.equals(node.getName(), name) && Objects.equals(node.getValueOrDefault(null), nodeData);
    }
}
