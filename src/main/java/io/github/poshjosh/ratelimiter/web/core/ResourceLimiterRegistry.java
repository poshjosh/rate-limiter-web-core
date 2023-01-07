package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import java.lang.reflect.Method;

public interface ResourceLimiterRegistry<R> extends Registries<R> {

    static <R> ResourceLimiterRegistry<R> ofDefaults() {
        return of(ResourceLimiterConfig.<R>builder().build());
    }

    static <R> ResourceLimiterRegistry<R> of(ResourceLimiterConfig<R> resourceLimiterConfig) {
        return new DefaultResourceLimiterRegistry<>(resourceLimiterConfig);
    }

    ResourceLimiter<R> createResourceLimiter();

    boolean isRateLimited(String id);

    RateLimitProperties properties();

    default boolean isRateLimited(Class<?> clazz) {
        return isRateLimited(ElementId.of(clazz));
    }

    default boolean isRateLimited(Method method) {
        return isRateLimited(ElementId.of(method));
    }

    default boolean isRateLimitingEnabled() {
        final Boolean disabled = properties().getDisabled();
        return disabled == null || Boolean.FALSE.equals(disabled);
    }
}
