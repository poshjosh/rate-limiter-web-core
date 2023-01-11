package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.Rate;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of {@link ResourceLimiter}s via properties.
 * The properties defined here will be used to create a {@link ResourceLimiter} independent
 * of those created from the various {@link Rate} annotations.
 */
public interface RateLimitProperties {

    /**
     * List of classes to search for resources annotated with rate limit related annotations.
     *
     * If using annotations, implement either this, {@link #getResourcePackages()}, or both.
     * If not using annotations, simply return an empty list.
     *
     * @return the packages containing resources having rate-limit related annotations.
     */
    List<Class<?>> getResourceClasses();

    /**
     * List of packages to search for resources annotated with rate limit related annotations.
     *
     * If using annotations, implement either this, {@link #getResourceClasses()}, or both.
     * If not using annotations, simply return an empty list.
     *
     * @return the packages containing resources having rate-limit related annotations.
     */
    List<String> getResourcePackages();

    /**
     * Should automatic rate limiting be disable?
     * @return {@code true} if automatic rate limiting should be disabled, otherwise return {@code false}
     */
    default Boolean getDisabled() {
        return Boolean.FALSE;
    }

    /**
     * If not using properties, simply return an empty map
     *
     * @return Configurations for building a customizable {@link ResourceLimiter}
     */
    Map<String, Rates> getRateLimitConfigs();
}
