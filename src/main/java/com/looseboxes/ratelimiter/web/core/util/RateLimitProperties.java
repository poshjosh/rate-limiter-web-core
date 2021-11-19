package com.looseboxes.ratelimiter.web.core.util;

import java.util.List;
import java.util.Map;

public interface RateLimitProperties {

    String getApplicationPath();

    List<String> getResourcePackages();

    Boolean getDisabled();

    Map<String, RateLimitConfigList> getRateLimitConfigs();
}
