package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Optional;

public interface RateLimiterRegistry {

    static RateLimiterRegistry ofDefaults() {
        return of(rateSource -> ResourceInfoProvider.ResourceInfo.of(PathPatterns.matchALL()));
    }

    static RateLimiterRegistry of(ResourceInfoProvider resourceInfoProvider) {
        return of(RateLimiterContext.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build());
    }

    static RateLimiterRegistry of(RateLimiterContext rateLimiterContext) {
        return new DefaultRateLimiterRegistry(rateLimiterContext);
    }

    /**
     * @param source The class to register as a rate limited resource
     * @throws IllegalArgumentException If the class is already registered,
     * or if it is already part of the hierarchy of nodes.
     */
    void register(Class<?> source) throws IllegalArgumentException;

    /**
     * @param source The method to register as a rate limited resource
     * @throws IllegalArgumentException If the method is already registered,
     * or if it is already part of the hierarchy of nodes.
     */
    void register(Method source) throws IllegalArgumentException;

    RateLimiterFactory<HttpServletRequest> createRateLimiterFactory();

    RateLimiterFactory<HttpServletRequest> createRateLimiterFactory(Class<?> clazz);

    RateLimiterFactory<HttpServletRequest> createRateLimiterFactory(Method method);

    Optional<RateLimiter> getRateLimiter(Class<?> clazz);

    Optional<RateLimiter> getRateLimiter(Method method);

    boolean isRateLimited(String id);

    default boolean isRateLimited(Class<?> clazz) {
        return isRateLimited(ElementId.of(clazz));
    }

    default boolean isRateLimited(Method method) {
        return isRateLimited(method.getDeclaringClass()) || isRateLimited(ElementId.of(method));
    }

    default boolean isRateLimitingEnabled() {
        final Boolean disabled = properties().getDisabled();
        return disabled == null || Boolean.FALSE.equals(disabled);
    }

    boolean hasMatching(String id);

    RateLimitProperties properties();

    UnmodifiableRegistries registries();
}
