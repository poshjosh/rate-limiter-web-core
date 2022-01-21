package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.util.RateConfigList;

import java.util.function.BiFunction;

public interface NodeValueConverter<K>
        extends BiFunction<String, NodeData<RateConfigList>, NodeData<RateLimiter<K>>> {

    @Override
    NodeData<RateLimiter<K>> apply(String name, NodeData<RateConfigList> nodeData);
}
