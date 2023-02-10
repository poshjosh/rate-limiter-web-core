package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.annotation.RateSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ResourceInfoProvider {

    ResourceInfo NONE = new ResourceInfo() {
        @Override public String getId() { return ""; }
        @Override public PathPatterns<String> getPathPatterns() { return PathPatterns.none(); }
        @Override public List<String> getHttpMethods() { return Collections.emptyList(); }
        @Override public String toString() { return "ResourceInfo$NONE"; }
    };

    interface ResourceInfo {

        ResourceInfo NONE = of();

        static ResourceInfo of(String... httpMethods) {
            return of(PathPatterns.none(), httpMethods);
        }

        static ResourceInfo of(PathPatterns<String> pathPatterns, String... httpMethods) {
            return new DefaultResourceInfo(pathPatterns, httpMethods);
        }

        String getId();

        PathPatterns<String> getPathPatterns();

        Collection<String> getHttpMethods();
    }

    ResourceInfo get(RateSource source);
}
