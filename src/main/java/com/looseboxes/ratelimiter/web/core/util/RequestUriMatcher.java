package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;

import java.util.Objects;

/**
 * Matcher which matches all request URIs
 * @param <R> The type of the request for which a match will be checked for
 */
public class RequestUriMatcher<R> implements Matcher<R, String>{

    private final RequestToIdConverter<R, String> requestToUriConverter;

    public RequestUriMatcher(RequestToIdConverter<R, String> requestToUriConverter) {
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
    }

    @Override
    public String getKeyIfMatchingOrDefault(R target, String resultIfNone) {
        return requestToUriConverter.convert(target);
    }
}
