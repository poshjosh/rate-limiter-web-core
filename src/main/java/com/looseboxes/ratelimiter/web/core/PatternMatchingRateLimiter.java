package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.BreadthFirstNodeVisitor;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PatternMatchingRateLimiter<R> implements RateLimiter<R>{

    private static final Logger log = LoggerFactory.getLogger(PatternMatchingRateLimiter.class);

    private final Predicate<R> filter;
    private final RateLimiterConfigurationSource<R> rateLimiterConfigurationSource;
    private final Node<NodeValue<RateLimiter<?>>> rootNode;
    private final List<Node<NodeValue<RateLimiter<?>>>> leafNodes;
    private final boolean firstMatchOnly;

    public PatternMatchingRateLimiter(Predicate<R> filter,
                                      RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
                                      Node<NodeValue<RateLimiter<?>>> rootNode,
                                      boolean firstMatchOnly) {
        this.filter = Objects.requireNonNull(filter);
        this.rateLimiterConfigurationSource = Objects.requireNonNull(rateLimiterConfigurationSource);
        this.rootNode = Objects.requireNonNull(rootNode);
        Set<Node<NodeValue<RateLimiter<?>>>> set = new LinkedHashSet<>();
        collectLeafNodes(this.rootNode, set::add);
        this.leafNodes = new LinkedList<>(set);
        this.firstMatchOnly = firstMatchOnly;
    }

    private <T> void collectLeafNodes(Node<T> root, Consumer<Node<T>> collector) {
        new BreadthFirstNodeVisitor<>(Node::isLeaf, collector).accept(root);
    }

    @Override
    public void increment(R request, int amount) {

        // We check this dynamically, to be able to respond to changes to this property dynamically
        if(!filter.test(request)) {
            return;
        }

        for(Node<NodeValue<RateLimiter<?>>> node : leafNodes) {

            final int matchCount = increment(request, amount, node);

            if(firstMatchOnly && matchCount > 0) {
                break;
            }
        }
    }

    private int increment(R request, int amount, Node<NodeValue<RateLimiter<?>>> node) {

        int successCount = 0;

        while(node != rootNode && node != null && node.hasNodeValue()) {

            final boolean success = doIncrement(request, amount, node);

            if(!success) {
                break;
            }

            ++successCount;

            node = node.getParentOrDefault(null);
        }

        return successCount;
    }

    private boolean doIncrement(R request, int amount, Node<NodeValue<RateLimiter<?>>> node) {

        final String nodeName = node.getName();
        final NodeValue<RateLimiter<?>> nodeValue = node.getValueOrDefault(null);
        final RateLimiter rateLimiter = nodeValue.getValue();
        log.trace("Name: {}, rate-limiter: {}", nodeName, rateLimiter);

        if(rateLimiter == RateLimiter.NO_OP) {
            return false;
        }

        Matcher<R, ?> matcher = getOrCreateMatcher(nodeName, nodeValue);

        final Object keyOrNull = matcher.getKeyIfMatchingOrDefault(request, null);

        final boolean matched = keyOrNull != null;
        if(log.isTraceEnabled()) {
            log.trace("Name: {}, matched: {}, matcher: {}", nodeName, matched, matcher);
        }

        if(!matched) {
            return false;
        }

        rateLimiter.increment(keyOrNull, amount);

        return true;
    }

    private Matcher<R, ?> getOrCreateMatcher(String name, NodeValue<RateLimiter<?>> nodeValue){
        return NodeUtil.isPropertyNodeData(nodeValue) ?
                rateLimiterConfigurationSource.getMatcherForProperties(name) :
                rateLimiterConfigurationSource.getMatcherForSourceElement(name, nodeValue.getSource());
    }
}
