package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.util.Nullable;

public interface Matcher<T, K> {

    Matcher<Object, Object> MATCH_NONE = (target, resultIfNone) -> resultIfNone;

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    default @Nullable K match(T target) {
        return getKeyIfMatchingOrDefault(target, null);
    }

    K getKeyIfMatchingOrDefault(T target, K resultIfNone);
}
