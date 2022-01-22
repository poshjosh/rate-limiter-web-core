package com.looseboxes.ratelimiter.web.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatcherTest {

    private final String key = "key";
    private final String success = "Success";
    private final String resultIfNone = "Failure";
    private final Matcher match = (k, v) -> success;
    private final Matcher noMatch = (k, v) -> v;

    @Test
    void matchThenNomatchShouldMatch() {
        testAndThen(match, match, success);
    }

    @Test
    void matchThenNomatchShouldNotMatch() {
        testAndThen(match, noMatch, resultIfNone);
    }

    @Test
    void nomatchThenMatchShouldNotMatch() {
        testAndThen(noMatch, match, resultIfNone);
    }

    @Test
    void nomatchThenNomatchShouldNotMatch() {
        testAndThen(noMatch, noMatch, resultIfNone);
    }

    private void testAndThen(Matcher lhs, Matcher rhs, Object expected) {
        final Matcher composed = lhs.andThen(rhs);
        final Object actual = composed.getIdIfMatchingOrDefault(key, resultIfNone);
        assertEquals(expected, actual);
    }
}