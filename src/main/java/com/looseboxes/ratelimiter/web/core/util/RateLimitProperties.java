package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.Limit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of a {@link com.looseboxes.ratelimiter.RateLimiter} via properties.
 * The properties defined here will be used to create a {@link com.looseboxes.ratelimiter.RateLimiter} independent
 * of those created from the various {@link com.looseboxes.ratelimiter.annotation.RateLimit} annotations.
 */
public interface RateLimitProperties {

    default Map<String, Limit> getLimits() {
        Map<String, RateLimitConfig> configs = getRateLimitConfigs();
        Map<String, Limit> limits = new HashMap<>(configs.size() * 4/3);
        for(String name : configs.keySet()) {
            limits.put(name, configs.get(name).toLimit());
        }
        return limits;
    }

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
     * @return Configurations for building a customizable {@link com.looseboxes.ratelimiter.RateLimiter}
     */
    Map<String, RateLimitConfig> getRateLimitConfigs();
}
