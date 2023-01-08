package io.github.poshjosh.ratelimiter.web.core.util;
/**
 * An additional match to be applied after path patterns have been matched.
 */
public enum MatchType {
    ATTRIBUTE,
    AUTH_SCHEME,
    COOKIE,
    HEADER,
    NOOP,
    PARAMETER,
    REMOTE_ADDRESS,

    /**
     * Format en-US, de-DE etc
     */
    LOCALE,

    USER_ROLE,
    USER_PRINCIPAL,
    REQUEST_URI,

    /** A Session will be created if none */
    SESSION_ID
}
