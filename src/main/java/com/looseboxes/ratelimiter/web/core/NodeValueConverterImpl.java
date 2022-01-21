package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.util.RateConfigList;

import java.util.Objects;
import java.util.function.BiPredicate;

public class NodeValueConverterImpl<R, K> implements NodeValueConverter<K> {

    private final BiPredicate<String, NodeData<RateConfigList>> filter;
    private final RateLimiterConfigurationSource<R> rateLimiterConfigurationSource;

    public NodeValueConverterImpl(
            BiPredicate<String, NodeData<RateConfigList>> filter,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource) {
        this.filter = Objects.requireNonNull(filter);
        this.rateLimiterConfigurationSource = Objects.requireNonNull(rateLimiterConfigurationSource);
    }

    @Override
    public NodeData<RateLimiter<K>> apply(String name, NodeData<RateConfigList> nodeData) {

        if(!filter.test(name, nodeData)) {

            return new NodeData<>(nodeData.getSource(), RateLimiter.noop());

        }else {

            RateConfigList config = nodeData.getValue();

            // One method with 3 @RateLimit annotations is a simple group (not really a group)
            // A true group spans either multiple methods/classes
            if(config.getLimits() == null || config.getLimits().isEmpty()) { // This is a group node

                // @TODO how do we handle this?
                // Do we create multiple rate limiters, one for each of the direct children of this group
                // Do we re-use the rate limiters of the children ? They must already exist since we create children first
                return new NodeData<>(nodeData.getSource(), RateLimiter.noop());

            }else {

                RateLimiter<K> rateLimiter = createRateLimiter(name, config);

                return new NodeData<>(nodeData.getSource(), rateLimiter);
            }
        }
    }


    private RateLimiter<K> createRateLimiter(String name, RateConfigList rateConfigList) {
        RateLimiterConfig rateLimiterConfig = rateLimiterConfigurationSource.copyConfigurationOrDefault(name);
        return rateLimiterConfigurationSource.getRateLimiterFactory(name)
                .createRateLimiter(rateLimiterConfig, rateConfigList);
    }
}
