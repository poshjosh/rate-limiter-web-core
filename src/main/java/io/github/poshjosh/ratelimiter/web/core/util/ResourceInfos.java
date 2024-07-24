package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.Collection;
import java.util.Collections;

public interface ResourceInfos {

    ResourceInfo NONE = new ResourceInfo() {
        @Override public String getId() {
            return "";
        }

        @Override public ResourcePath<String> getResourcePath() {
            return ResourcePaths.matchNone();
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
        return of(ResourcePaths.matchNone(), httpMethods);
    }

    static ResourceInfo of(ResourcePath<String> resourcePath, String... httpMethods) {
        if (ResourcePaths.matchNone().equals(resourcePath) && (httpMethods == null
                || httpMethods.length == 0)) {
            return none();
        }
        return new DefaultResourceInfo(resourcePath, httpMethods);
    }
}
