package com.looseboxes.ratelimiter.web.core.util;

public interface Matcher<T> {

    Matcher<Object> MATCH_NONE = target -> false;

    @SuppressWarnings("unchecked")
    static <T> Matcher<T> matchNone() {
        return (Matcher<T>)MATCH_NONE;
    }

    boolean matches(T target);

    default Object getId(T target) {
        return target;
    }
}
