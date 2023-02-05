package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatternsProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public interface ResourceLimiterRegistry<R> {

    static ResourceLimiterRegistry<HttpServletRequest> of(PathPatternsProvider pathPatternsProvider) {
        return of(ResourceLimiterConfig.builder(HttpServletRequest.class)
                .pathPatternsProvider(pathPatternsProvider)
                .build());
    }

    static <R> ResourceLimiterRegistry<R> of(ResourceLimiterConfig<R> resourceLimiterConfig) {
        return new DefaultResourceLimiterRegistry<>(resourceLimiterConfig);
    }

    ResourceLimiter<R> createResourceLimiter();

    boolean isRateLimited(String id);

    List<RateLimiter> getRateLimiters(String id);

    Optional<RateConfig> getRateConfig(String id);

    RateLimitProperties properties();

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

    /**
     * Return the Matchers registered to {@link Registries#matchers()}
     *
     * Registration is done by calling any of the <code>register</code> methods of the returned
     * {@link io.github.poshjosh.ratelimiter.web.core.Registry}
     *
     * @return The registered matchers
     * @see #getMatchers(String)
     */
    UnmodifiableRegistry<Matcher<R>> matchers();

    default boolean hasMatching(String id) {
        return getMatchers(id).stream().anyMatch(matcher -> !Matcher.matchNone().equals(matcher));
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     * @see #matchers()
     */
    List<Matcher<R>> getMatchers(String id);

    Optional<BandwidthsStore<?>> getStore();

    Optional<UsageListener> getListener();
}
