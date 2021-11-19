package com.looseboxes.ratelimiter.web.core;

@FunctionalInterface
public interface RequestToIdConverter<R> {
    Object convert(R request);
}
