package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.RequestToIdConverter;

import java.util.Objects;

/**
 * Matcher to match the path patterns declared on an element.
 *
 * @param <R> The type of the request for which a match will be checked for
 */
public class PathPatternsMatcher<R> implements Matcher<R, PathPatterns<String>> {

    private final PathPatterns<String> pathPatterns;

    private final RequestToIdConverter<R, String> requestToUriConverter;

    public PathPatternsMatcher(
            PathPatterns<String> pathPatterns,
            RequestToIdConverter<R, String> requestToUriConverter) {

        this.pathPatterns = Objects.requireNonNull(pathPatterns);

        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public PathPatterns<String> matchOrNull(R target) {
        return matches(target) ? pathPatterns : null;
    }

    @Override
    public boolean matches(R request) {
        String uri = requestToUriConverter.toId(request);
        return pathPatterns.matches(uri);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PathPatternsMatcher<?> that = (PathPatternsMatcher<?>)o;
        return pathPatterns.equals(that.pathPatterns);
    }

    @Override public int hashCode() {
        return Objects.hash(pathPatterns);
    }

    @Override
    public String toString() {
        return "PathPatternsMatcher{" + pathPatterns.getPatterns() + "}";
    }
}
