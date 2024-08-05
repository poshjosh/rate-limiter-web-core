package io.github.poshjosh.ratelimiter.web.core;

public interface WebExpressionKey {

    String ATTRIBUTE = "web.request.attribute";
    String AUTH_SCHEME = "web.request.auth.scheme";
    String COOKIE = "web.request.cookie";
    String HEADER = "web.request.header";
    /**
     * The ip address of the request.
     * Alias for {@link #REMOTE_ADDRESS}
     */
    String IP = "web.request.ip";

    /**
     * Format en-US, de-DE etc
     */
    String LOCALE = "web.request.locale";
    String PARAMETER = "web.request.parameter";
    /**
     * The ip address of the request.
     * Alias for {@link #IP}
     */
    String REMOTE_ADDRESS = "web.request.remote.address";
    String REQUEST_URI = "web.request.uri";
    String USER_ROLE = "web.request.user.role";
    String USER_PRINCIPAL = "web.request.user.principal";

    /** A Session will be created if none */
    String SESSION_ID = "web.session.id";

    static boolean isKeyValueType(String key) {
        return key.startsWith(ATTRIBUTE) || key.startsWith(COOKIE) ||
                key.startsWith(HEADER) || key.startsWith(PARAMETER);
    }

    static boolean isKey(String key) {
        return key.startsWith(ATTRIBUTE) || key.startsWith(AUTH_SCHEME)
        || key.startsWith(COOKIE) || key.startsWith(HEADER)
        || key.startsWith(IP) || key.startsWith(LOCALE)
        || key.startsWith(PARAMETER) || key.startsWith(REMOTE_ADDRESS)
        || key.startsWith(REQUEST_URI) || key.startsWith(USER_ROLE)
        || key.startsWith(USER_PRINCIPAL) || key.startsWith(SESSION_ID);
    }
}
