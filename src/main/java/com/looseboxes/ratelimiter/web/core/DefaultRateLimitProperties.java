package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class DefaultRateLimitProperties implements RateLimitProperties {
    DefaultRateLimitProperties() { }
    @Override public List<String> getResourcePackages() {
        return Collections.emptyList();
    }
    @Override public Map<String, Rates> getRateLimitConfigs() {
        return Collections.emptyMap();
    }
}
