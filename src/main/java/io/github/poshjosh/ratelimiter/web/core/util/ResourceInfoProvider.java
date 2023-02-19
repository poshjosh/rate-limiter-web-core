package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.annotation.RateSource;

import java.util.Collection;

public interface ResourceInfoProvider {

    ResourceInfoProvider NONE = source -> ResourceInfo.NONE;

    interface ResourceInfo {

        ResourceInfo NONE = of();

        static ResourceInfo of(String... httpMethods) {
            return of(PathPatterns.matchNone(), httpMethods);
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
