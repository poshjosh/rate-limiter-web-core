package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.BreadthFirstNodeVisitor;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialArray;
import java.io.Serializable;
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

            Node<NodeValue<RateLimiter<?>>> currentNode = node;
            NodeValue<RateLimiter<?>> nodeValue =  currentNode.getValueOrDefault(null);

            int matchCount = 0;

            while(currentNode != rootNode && nodeValue != null) {

                RateLimiter<Serializable> rateLimiter = nodeValue.getValue();
                final String currentNodeName = currentNode.getName();
                log.trace("Name: {}, rate-limiter: {}", currentNodeName, rateLimiter);

                if(rateLimiter == RateLimiter.NO_OP) {
                    break;
                }

                Matcher<R, ?> matcher = getOrCreateMatcher(currentNodeName, nodeValue);

                final Object keyOrNull = matcher.getKeyIfMatchingOrDefault(request, null);

                final boolean matched = keyOrNull != null;
                if(log.isTraceEnabled()) {
                    log.trace("Name: {}, matched: {}, matcher: {}", currentNodeName, matched, matcher);
                }

                if(matched) {
                    rateLimiter.increment(keyOrNull, amount);
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

    private <K extends Serializable> Matcher<R, K> getOrCreateMatcher(String name, NodeValue<RateLimiter<K>> nodeValue){
        return NodeUtil.isPropertyNodeData(nodeValue) ?
                rateLimiterConfigurationSource.getMatcherForProperties(name) :
                rateLimiterConfigurationSource.getMatcherForSourceElement(name, nodeValue.getSource());
    }
}
