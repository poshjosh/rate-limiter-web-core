package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.util.PathPatterns;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public interface ResourceLimiterRegistry {

    static ResourceLimiterRegistry ofDefaults() {
        return of(rateSource -> ResourceInfoProvider.ResourceInfo.of(PathPatterns.matchALL()));
    }

    static ResourceLimiterRegistry of(ResourceInfoProvider resourceInfoProvider) {
        return of(ResourceLimiterConfig.builder()
                .resourceInfoProvider(resourceInfoProvider)
                .build());
    }

    static ResourceLimiterRegistry of(ResourceLimiterConfig resourceLimiterConfig) {
        return new DefaultResourceLimiterRegistry(resourceLimiterConfig);
    }

    boolean register(Class<?> source);

    boolean register(Method source);

    Matcher<HttpServletRequest> getOrCreateMainMatcher(Class<?> clazz);

    Matcher<HttpServletRequest> getOrCreateMainMatcher(Method method);

    List<Matcher<HttpServletRequest>> getOrCreateSubMatchers(Class<?> clazz);

    List<Matcher<HttpServletRequest>> getOrCreateSubMatchers(Method method);

    ResourceLimiter<HttpServletRequest> createResourceLimiter();

    ResourceLimiter<HttpServletRequest> createResourceLimiter(Class<?> clazz);

    ResourceLimiter<HttpServletRequest> createResourceLimiter(Method method);

    default boolean hasMatching(String id) {
        return getMatchers(id).stream().anyMatch(matcher -> !Matcher.matchNone().equals(matcher));
    }

    /**
     * @param clazz The class bearing the matchers to return
     * @return All the matchers that will be applied for the given id
     * @see #getMatchers(String)
     */
    default List<Matcher<HttpServletRequest>> getMatchers(Class<?> clazz) {
        return getMatchers(ElementId.of(clazz));
    }

    /**
     * @param method The method bearing the matchers to return
     * @return All the matchers that will be applied for the given id
     * @see #getMatchers(String)
     */
    default List<Matcher<HttpServletRequest>> getMatchers(Method method) {
        return getMatchers(ElementId.of(method));
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     */
    List<Matcher<HttpServletRequest>> getMatchers(String id);

    List<RateLimiter> createRateLimiters(Class<?> clazz);

    List<RateLimiter> createRateLimiters(Method method);

    LimiterConfig<HttpServletRequest> createConfig(Class<?> source);

    LimiterConfig<HttpServletRequest> createConfig(Method source);

    default Optional<LimiterConfig<HttpServletRequest>> getConfig(Class<?> clazz) {
        return getConfig(ElementId.of(clazz));
    }

    default Optional<LimiterConfig<HttpServletRequest>> getConfig(Method method) {
        return getConfig(ElementId.of(method));
    }

    Optional<LimiterConfig<HttpServletRequest>> getConfig(String id);

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

    RateLimitProperties properties();

    UnmodifiableRegistries registries();
}
