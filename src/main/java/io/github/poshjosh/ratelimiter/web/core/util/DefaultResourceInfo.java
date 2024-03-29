package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.*;

final class DefaultResourceInfo implements ResourceInfoProvider.ResourceInfo {
    private final Collection<String> httpMethods;
    private final PathPatterns<String> pathPatterns;
    private final String id;
    DefaultResourceInfo(PathPatterns<String> pathPatterns, String...httpMethods) {
        this.httpMethods = toCollection(httpMethods);
        this.pathPatterns = Objects.requireNonNull(pathPatterns);
        final List<String> paths = getPathPatterns().getPatterns();
        this.id = paths.isEmpty() ? this.httpMethods.toString() : this.httpMethods.toString() + paths;
    }
    private Collection<String> toCollection(String...arr) {
        return arr.length == 0 ? Collections.emptyList() : arr.length == 1 ?
                Collections.singletonList(arr[0]) : Collections.unmodifiableList(Arrays.asList(arr));
    }
    @Override public String getId() { return id; }
    @Override public Collection<String> getHttpMethods() { return httpMethods; }
    @Override public PathPatterns<String> getPathPatterns() { return pathPatterns; }
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DefaultResourceInfo that = (DefaultResourceInfo) o;
        return httpMethods.equals(that.httpMethods) && pathPatterns.equals(that.pathPatterns);
    }
    @Override public int hashCode() {
        return Objects.hash(httpMethods, pathPatterns);
    }
    @Override public String toString() { return "ResourceInfo{" + id + '}'; }
}
