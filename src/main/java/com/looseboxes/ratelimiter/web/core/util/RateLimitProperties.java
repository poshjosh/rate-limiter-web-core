package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.annotations.Rate;
import com.looseboxes.ratelimiter.util.Rates;

import java.util.List;
import java.util.Map;

/**
 * The class allows for the customization of a {@link com.looseboxes.ratelimiter.ResourceLimiter} via properties.
 * The properties defined here will be used to create a {@link com.looseboxes.ratelimiter.ResourceLimiter} independent
 * of those created from the various {@link Rate} annotations.
 */
public interface RateLimitProperties {

    /**
     * If using annotations, you have to specify the list packages where resources containing
     * rate-limit related annotations should be scanned for.
     *
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
     * @return Configurations for building a customizable {@link com.looseboxes.ratelimiter.ResourceLimiter}
     */
    Map<String, Rates> getRateLimitConfigs();
}
