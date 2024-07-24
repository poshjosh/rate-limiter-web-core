package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.*;

final class DefaultResourceInfo implements ResourceInfo {
    private final Collection<String> httpMethods;
    private final ResourcePath<String> resourcePath;
    private final String id;
    DefaultResourceInfo(ResourcePath<String> resourcePath, String...httpMethods) {
        this.httpMethods = toCollection(httpMethods);
        this.resourcePath = Objects.requireNonNull(resourcePath);
        final List<String> paths = getResourcePath().getPatterns();
        this.id = paths.isEmpty() ? this.httpMethods.toString() : this.httpMethods.toString() + paths;
    }
    private Collection<String> toCollection(String...arr) {
        return arr.length == 0 ? Collections.emptyList() : arr.length == 1 ?
                Collections.singletonList(arr[0]) : Collections.unmodifiableList(Arrays.asList(arr));
    }
    @Override public String getId() { return id; }
    @Override public Collection<String> getHttpMethods() { return httpMethods; }
    @Override public ResourcePath<String> getResourcePath() { return resourcePath; }
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DefaultResourceInfo that = (DefaultResourceInfo) o;
        return httpMethods.equals(that.httpMethods) && resourcePath.equals(that.resourcePath);
    }
    @Override public int hashCode() {
        return Objects.hash(httpMethods, resourcePath);
    }
    @Override public String toString() { return "ResourceInfo{" + id + '}'; }
}
