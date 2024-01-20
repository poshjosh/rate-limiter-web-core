package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.model.RateSource;

import java.util.Collection;
import java.util.Collections;

public interface ResourceInfoProvider {

    ResourceInfoProvider NONE = source -> ResourceInfo.NONE;

    interface ResourceInfo {

        ResourceInfo NONE = new ResourceInfo() {
            @Override public String getId() {
                return "";
            }
            @Override public PathPatterns<String> getPathPatterns() {
                return PathPatterns.matchNone();
            }
            @Override public Collection<String> getHttpMethods() {
                return Collections.emptyList();
            }
            @Override public String toString() {
                return "ResourceInfoProvider$NONE";
            }
        };


        @SuppressWarnings("unchecked")
        static ResourceInfo none() {
            return NONE;
        }

        static ResourceInfo of(String... httpMethods) {
            return of(PathPatterns.matchNone(), httpMethods);
        }

        static ResourceInfo of(PathPatterns<String> pathPatterns, String... httpMethods) {
            if (PathPatterns.matchNone().equals(pathPatterns)
                    && (httpMethods == null || httpMethods.length == 0)) {
                return none();
            }
            return new DefaultResourceInfo(pathPatterns, httpMethods);
        }

        String getId();

        PathPatterns<String> getPathPatterns();

        Collection<String> getHttpMethods();
    }

    ResourceInfo get(RateSource source);
}
