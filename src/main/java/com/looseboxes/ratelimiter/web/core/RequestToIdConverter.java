package com.looseboxes.ratelimiter.web.core;

@FunctionalInterface
public interface RequestToIdConverter<R, K> {
    K convert(R request);
}
