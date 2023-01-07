package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.annotation.Element;

public interface PathPatternsProvider {
    PathPatterns<String> get(Element source);
}
