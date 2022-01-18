package com.looseboxes.ratelimiter.web.core.util;

import java.util.Objects;

public interface Matcher<T, R> {

    Matcher<Object, Object> MATCH_NONE = (target, resultIfNone) -> resultIfNone;

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    R getKeyIfMatchingOrDefault(T target, R resultIfNone);

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
    default Matcher<T, R> andThen(Matcher<? super T, ? super R> after) {
        Objects.requireNonNull(after);
        return (T t, R r) -> {
            R result = getKeyIfMatchingOrDefault(t, r);
            // If there was no match, do not continue
            if(result == r) {
                return result;
            }
            return (R)after.getKeyIfMatchingOrDefault(t, r);
        };
    }

}
