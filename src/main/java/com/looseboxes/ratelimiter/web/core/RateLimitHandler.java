package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.DefaultRateLimiter;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfiguration;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.cache.SingletonRateCache;
import com.looseboxes.ratelimiter.node.*;
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
import java.util.stream.Collectors;

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
        final Node<NodeData> elementRoot = NodeUtil.addNodesToRoot(properties.getRateLimitConfigs());
        final Set<String> propertyGroupNames = new LinkedHashSet<>();
        collectLeafNodes(elementRoot, node -> propertyGroupNames.add(node.getName()));

        final BiConsumer<Object, Node<NodeData>> requirePropertyGroupNameNotUsedAsAnnotationGroupName = (element, node) -> {
            if(node != null && propertyGroupNames.contains(node.getName())) {
                throw new IllegalStateException(
                        "The same name cannot be used for both property based and annotation based rate limit group. Name: " + node.getName());
            }
        };

        // Add annotation based rate limit groups
        annotationProcessor.process(elementRoot, resourceClasses, requirePropertyGroupNameNotUsedAsAnnotationGroupName);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Element Nodes: {}", new NodeFormatter().format(elementRoot));
        }

        BiFunction<String, NodeData, RateLimiter<R>> valueConverter =
                (name, nodeData) -> createRateLimiter(name, nodeData, rateLimiterConfigurationSource);

        // Transform the root and it's children to rate limiter nodes
        Node<RateLimiter<R>> rateLimiterRoot = elementRoot.transform(null, (name, value) -> name, valueConverter);
        if(LOG.isDebugEnabled()) {
            LOG.debug("RateLimiter Nodes: {}", new NodeFormatter().format(rateLimiterRoot));
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

//        if(LOG.isTraceEnabled()) {
//            LOG.trace("\nProperty based rate limiter leaf nodes:\n{}\nAnnotation based rate limiter leaf nodes:\n{}",
//                    propertyBasedRateLimiterLeafNodes.stream().map(NodeUtil::toString).collect(Collectors.joining("\n")),
//                    annotationBasedRateLimiterLeafNodes.stream().map(NodeUtil::toString).collect(Collectors.joining("\n")));
//        }
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new NodeVisitor<>(Node::isLeaf, collector).accept(root);
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

//        System.out.println();

        for(Node<RateLimiter<R>> rateLimiterNode : rateLimiterNodes) {

            Node<RateLimiter<R>> currentNode = rateLimiterNode;
            RateLimiter<R> rateLimiter =  currentNode.getValueOrDefault(null);

            int matchCount = 0;

            while(rateLimiter != null && rateLimiter != RateLimiter.noop()) {

//                System.out.println("RateLimitHandler using " + currentNode.getName() + " = " + rateLimiter);

                final Rate rate = rateLimiter.record(request);

//                System.out.println("RateLimitHandler rate " + rate);

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

    private <R> RateLimiter<R> createRateLimiter(
            String name,
            NodeData nodeData,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource){
        if(NodeUtil.isRootNode(name, nodeData)) {
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
                        rateLimiterConfigurationSource.copyConfigurationOrDefault(name)
                                .rateLimitConfig(config);

                Matcher<R> matcher = getOrCreateMatcher(name, nodeData, rateLimiterConfigurationSource);

                // Since each PathPatterns pertains to one RateLimiter, we use SingletonRateCache
                // This is because the PathPatterns is used as the key. Irrespective of which request URI is received
                // we use the matching PathPatterns as key. This obviates the need for multi key rate cache.
                // @TODO Find a better way to achieve this
                // This is overriding registered RateCache
                if(!NodeUtil.isPropertyNodeData(nodeData)) {
                    // @TODO fix this
                    // Passing null to method Matcher.getId() is an ugly hack
                    rateLimiterConfiguration.rateCache(new SingletonRateCache<>(matcher.getId(null)));
                }

                return new RequestMatchingRateLimiter<>(matcher, new DefaultRateLimiter<>(rateLimiterConfiguration));
            }
        }
    }


    private <R> Matcher<R> getOrCreateMatcher(
            String name,
            NodeData nodeData,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource){

        return NodeUtil.isRootNode(name, nodeData) ? Matcher.matchNone() :
                NodeUtil.isPropertyNodeData(nodeData) ? rateLimiterConfigurationSource.getMatcherForProperties(name) :
                        rateLimiterConfigurationSource.getMatcherForSource(name, nodeData.getSource());
    }}
