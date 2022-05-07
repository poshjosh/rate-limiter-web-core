package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.*;
import com.looseboxes.ratelimiter.node.*;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class WebRequestRateLimiter<R> implements RateLimiter<R>{

    private final RateLimiter<R> rateLimiterFromProperties;
    private final RateLimiter<R> rateLimiterFromAnnotations;

    public WebRequestRateLimiter(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            RateLimiterNodeContext<R, ?> context) {

        this.rateLimiterFromProperties = createRateLimiter(
                properties, rateLimiterConfigurationSource, (Node)context.getPropertiesRoot(), false);

        this.rateLimiterFromAnnotations = createRateLimiter(
                properties, rateLimiterConfigurationSource, (Node)context.getAnnotationsRoot(), true);
    }

    @Override
    public boolean increment(Object source, R request, int amount) {
        int failCount = 0;
        try {
            if(!this.rateLimiterFromProperties.increment(source, request, amount)) {
                ++failCount;
            }
        }finally {
            if(!this.rateLimiterFromAnnotations.increment(source, request, amount)) {
                ++failCount;
            }
        }
        return failCount == 0;
    }

    private RateLimiter<R> createRateLimiter(
            RateLimitProperties properties,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            Node<NodeData<RateLimiter<?>>> rateLimiterRootNode,
            boolean annotations) {

        Predicate<R> filter = request -> !Boolean.TRUE.equals(properties.getDisabled());

        BiFunction<String, NodeData<RateLimiter<?>>, Matcher<R, ?>> matcherProvider =
                getMatcherProvider(rateLimiterConfigurationSource, annotations);

        return new PatternMatchingRateLimiter<>(filter, matcherProvider, rateLimiterRootNode, annotations);
    }

    private BiFunction<String, NodeData<RateLimiter<?>>, Matcher<R, ?>> getMatcherProvider(
            MatcherRegistry<R> matcherRegistry, boolean annotations){
        return !annotations ?
                (name, nodeData) -> matcherRegistry.getOrCreateMatcherForProperties(name) :
                (name, nodeData) -> matcherRegistry.getOrCreateMatcherForSourceElement(name, nodeData.getSource());
    }
}
