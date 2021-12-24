package com.looseboxes.ratelimiter.web.core.util;

public interface Matcher<T, K> {

    Matcher<Object, Object> MATCH_NONE = (target, resultIfNone) -> resultIfNone;

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    K getKeyIfMatchingOrDefault(T target, K resultIfNone);
}
