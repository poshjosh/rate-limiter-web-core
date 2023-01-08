package io.github.poshjosh.ratelimiter.web.core;

@FunctionalInterface
public interface RequestToIdConverter<R, K> {
    K toId(R request);
}
