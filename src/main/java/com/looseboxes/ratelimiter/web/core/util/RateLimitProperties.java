package com.looseboxes.ratelimiter.web.core.util;

import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of a {@link com.looseboxes.ratelimiter.RateLimiter} via properties.
 * The properties defined here will be used to create a {@link com.looseboxes.ratelimiter.RateLimiter} independent
 * of those created from the various {@link com.looseboxes.ratelimiter.annotation.RateLimit} annotations.
 */
public interface RateLimitProperties {

    /**
     * Should the {@link com.looseboxes.ratelimiter.RateLimiter} created from
     * {@link com.looseboxes.ratelimiter.web.core.util.RateLimitProperties} be automatically deployed?
     *
     * If {code false} is returned, then the {@link com.looseboxes.ratelimiter.RateLimiter} created from
     * {@link com.looseboxes.ratelimiter.web.core.util.RateLimitProperties} has to be injected by the user
     * in such a way that the {@link com.looseboxes.ratelimiter.RateLimiter#record(Object)} method
     * is called once per request.
     *
     * @return {@code true} if the {@link com.looseboxes.ratelimiter.RateLimiter} created from
     * {@link com.looseboxes.ratelimiter.web.core.util.RateLimitProperties} be automatically deployed, otherwise
     * return {@code false}
     */
    default Boolean getAuto() {
        return Boolean.TRUE;
    }

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
     * @return Configuration for building a customizable {@link com.looseboxes.ratelimiter.RateLimiter}
     */
    Map<String, RateConfigList> getRateLimitConfigs();
}
