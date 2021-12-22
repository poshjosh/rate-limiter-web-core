package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.RateLimitConfig;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiFunction;

public class NodeValueConverter implements BiFunction<String, NodeValue<RateLimitConfig>, NodeValue<RateLimiter<? extends Serializable>>> {

    private final Node<NodeValue<RateLimitConfig>> rootNode;
    private final RateLimiterConfigurationSource<?> rateLimiterConfigurationSource;

    public NodeValueConverter(Node<NodeValue<RateLimitConfig>> rootNode,
                              RateLimiterConfigurationSource<?> rateLimiterConfigurationSource) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.rateLimiterConfigurationSource = Objects.requireNonNull(rateLimiterConfigurationSource);
    }

    @Override
    public NodeValue<RateLimiter<? extends Serializable>> apply(String name, NodeValue<RateLimitConfig> nodeValue) {

        if(isEqual(rootNode, name, nodeValue)) {

            return new NodeValue<>(nodeValue.getSource(), RateLimiter.noop());

        }else {

            RateLimitConfig config = nodeValue.getValue();

            // One method with 3 @RateLimit annotations is a simple group (not really a group)
            // A true group spans either multiple methods/classes
            if(config.getLimits() == null || config.getLimits().isEmpty()) { // This is a group node

                // @TODO how do we handle this?
                // Do we create multiple rate limiters, one for each of the direct children of this group
                // Do we re-use the rate limiters of the children ? They must already exist since we create children first
                return new NodeValue<>(nodeValue.getSource(), RateLimiter.noop());

            }else {

                RateLimiter<? extends Serializable> rateLimiter = rateLimiterConfigurationSource.createRateLimiter(name, config);

                return new NodeValue<>(nodeValue.getSource(), rateLimiter);
            }
        }
    }

    private <V> boolean isEqual(Node<NodeValue<V>> node, String name, NodeValue<V> nodeValue) {
        return Objects.equals(node.getName(), name) && Objects.equals(node.getValueOrDefault(null), nodeValue);
    }
}
