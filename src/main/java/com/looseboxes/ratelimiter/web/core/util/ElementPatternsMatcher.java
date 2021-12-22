package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;

import java.lang.reflect.GenericDeclaration;
import java.util.Objects;

/**
 * Matcher to match the path patterns declared on an element.
 *
 * @param <S> The type of the element whose path patterns will be matched
 * @param <R> The type of the request for which a match will be checked for
 */
public class ElementPatternsMatcher<S extends GenericDeclaration, R> implements Matcher<R, PathPatterns<String>>{

    private final PathPatterns<String> pathPatterns;

    private final RequestToIdConverter<R, String> requestToUriConverter;

    public ElementPatternsMatcher(
            S source,
            IdProvider<S, PathPatterns<String>> pathPatternsProvider,
            RequestToIdConverter<R, String> requestToUriConverter) {

        this.pathPatterns = Objects.requireNonNull(pathPatternsProvider.getId(source));

        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public PathPatterns<String> getKeyIfMatchingOrDefault(R target, PathPatterns<String> resultIfNone) {
        return matches(target) ? pathPatterns : resultIfNone;
    }

    private boolean matches(R request) {
        String uri = requestToUriConverter.convert(request);
        return pathPatterns.matches(uri);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ElementPatternsMatcher<?, ?> that = (ElementPatternsMatcher<?, ?>) o;
        return pathPatterns.equals(that.pathPatterns);
    }

    @Override public int hashCode() {
        return Objects.hash(pathPatterns);
    }

    @Override
    public String toString() {
        return "ElementPatternsMatcher{" + pathPatterns.getPatterns() + "}";
    }
}
