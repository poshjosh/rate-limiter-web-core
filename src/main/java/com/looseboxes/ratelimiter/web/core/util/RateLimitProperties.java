package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.util.Rates;

import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of a {@link ResourceLimiter} via properties.
 * The properties defined here will be used to create a {@link ResourceLimiter} independent
 * of those created from the various {@link RateLimit} annotations.
 */
public interface RateLimitProperties {

    /**
     * @return The packages to search for resource classes
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
     * @return Configurations for building a customizable {@link ResourceLimiter}
     */
    Map<String, Rates> getRateLimitConfigs();
}
