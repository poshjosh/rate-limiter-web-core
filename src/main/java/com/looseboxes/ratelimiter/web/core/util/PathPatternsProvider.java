package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.annotation.Element;

public interface PathPatternsProvider {
    PathPatterns<String> get(Element source);
}
