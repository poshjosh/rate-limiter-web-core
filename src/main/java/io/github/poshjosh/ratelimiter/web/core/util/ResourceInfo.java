package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.Collection;

public interface ResourceInfo {

    String getId();

    ResourcePath<String> getResourcePath();

    Collection<String> getHttpMethods();
}
