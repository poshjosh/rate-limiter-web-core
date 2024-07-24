package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.Collection;

public interface ResourceInfo {

    String getId();

    ResourcePath getResourcePath();

    Collection<String> getHttpMethods();
}
