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

    private enum RateLimitResult{SUCCESS, FAILURE, NOMATCH, NOOP}

    private static final Logger log = LoggerFactory.getLogger(PatternMatchingRateLimiter.class);

    private final Predicate<R> filter;
    private final MatcherRegistry<R> matcherRegistry;
    private final Node<NodeValue<RateLimiter<?>>> rootNode;
    private final List<Node<NodeValue<RateLimiter<?>>>> leafNodes;
    private final boolean firstMatchOnly;

    public PatternMatchingRateLimiter(Predicate<R> filter,
                                      MatcherRegistry<R> matcherRegistry,
                                      Node<NodeValue<RateLimiter<?>>> rootNode,
                                      boolean firstMatchOnly) {
        this.filter = Objects.requireNonNull(filter);
        this.matcherRegistry = Objects.requireNonNull(matcherRegistry);
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
    public boolean increment(R request, int amount) {

        // We check this dynamically, to be able to respond to changes to this property dynamically
        if(!filter.test(request)) {
            return true;
        }

        int globalFailureCount = 0;

        for(Node<NodeValue<RateLimiter<?>>> node : leafNodes) {

            int nodeSuccessCount = 0;

            while(node != rootNode && node != null && node.hasNodeValue()) {

                final RateLimitResult result = increment(request, amount, node);

                switch(result) {
                    case SUCCESS: ++nodeSuccessCount; break;
                    case FAILURE: ++globalFailureCount; break;
                    case NOMATCH:
                    case NOOP:
                        break;
                    default: throw new IllegalArgumentException();
                }

                if(!RateLimitResult.SUCCESS.equals(result)) {
                    break;
                }

                node = node.getParentOrDefault(null);
            }

            if(firstMatchOnly && nodeSuccessCount > 0) {
                break;
            }
        }

        return globalFailureCount == 0;
    }

    private RateLimitResult increment(R request, int amount, Node<NodeValue<RateLimiter<?>>> node) {

        final String nodeName = node.getName();
        final NodeValue<RateLimiter<?>> nodeValue = node.getValueOrDefault(null);
        final RateLimiter rateLimiter = nodeValue.getValue();
        if(log.isTraceEnabled()) {
            log.trace("Name: {}, rate-limiter: {}", nodeName, rateLimiter);
        }

        if(rateLimiter == RateLimiter.NO_OP) {
            return RateLimitResult.NOOP;
        }

        Matcher<R, ?> matcher = getOrCreateMatcher(nodeName, nodeValue);

        final Object key = matcher.getKeyIfMatchingOrDefault(request, null);

        final boolean matched = key != null;
        if(log.isTraceEnabled()) {
            log.trace("Name: {}, matched: {}, matcher: {}", nodeName, matched, matcher);
        }

        if(!matched) {
            return RateLimitResult.NOMATCH;
        }

        return rateLimiter.increment(key, amount) ? RateLimitResult.SUCCESS : RateLimitResult.FAILURE;
    }

    private Matcher<R, ?> getOrCreateMatcher(String name, NodeValue<RateLimiter<?>> nodeValue){
        return NodeUtil.isPropertyNodeData(nodeValue) ?
                matcherRegistry.getOrCreateMatcherForProperties(name) :
                matcherRegistry.getOrCreateMatcherForSourceElement(name, nodeValue.getSource());
    }
}
