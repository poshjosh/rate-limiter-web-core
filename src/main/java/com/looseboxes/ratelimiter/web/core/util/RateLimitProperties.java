package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.util.RateConfigList;

import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of a {@link com.looseboxes.ratelimiter.RateLimiter} via properties.
 * The properties defined here will be used to create a {@link com.looseboxes.ratelimiter.RateLimiter} independent
 * of those created from the various {@link com.looseboxes.ratelimiter.annotation.RateLimit} annotations.
 */
public interface RateLimitProperties {

    /**
     * @return The packages to search for resource classes
     */
    List<String> getResourcePackages();

    /**
     * Should rate limiting be disable?
     * @return {@code true} if rate limiting should be disabled, otherwise return {@code false}
     */
    default Boolean getDisabled() {
        return Boolean.FALSE;
    }

    /**
     * @return Configurations for building a customizable {@link com.looseboxes.ratelimiter.RateLimiter}
     */
    Map<String, RateConfigList> getRateLimitConfigs();
}
