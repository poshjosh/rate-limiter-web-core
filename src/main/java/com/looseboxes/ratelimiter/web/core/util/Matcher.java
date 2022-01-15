package com.looseboxes.ratelimiter.web.core.util;

import java.util.Objects;

public interface Matcher<T, K> {

    Matcher<Object, Object> MATCH_NONE = (target, resultIfNone) -> resultIfNone;

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    K getKeyIfMatchingOrDefault(T target, K resultIfNone);

    /**
     * Returns a composed {@code Matcher} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Matcher} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default Matcher<T, K> andThen(Matcher<? super T, ? super K> after) {
        Objects.requireNonNull(after);
        return (T t, K k) -> {
            K result = getKeyIfMatchingOrDefault(t, k);
            // If there was no match, do not continue
            if(result == k) {
                return result;
            }
            return (K)after.getKeyIfMatchingOrDefault(t, k);
        };
    }

}
