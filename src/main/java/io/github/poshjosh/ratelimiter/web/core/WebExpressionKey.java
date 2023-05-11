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

    static boolean isNameValueType(String key) {
        switch(key) {
            case ATTRIBUTE:
            case AUTH_SCHEME:
            case COOKIE:
            case HEADER:
            case PARAMETER:
            case USER_ROLE:
                return true;
            default :
                return false;
        }
    }

    static boolean isKey(String expression) {
        switch(expression) {
            case ATTRIBUTE:
            case AUTH_SCHEME:
            case COOKIE:
            case HEADER:
            case IP:
            case LOCALE:
            case PARAMETER:
            case REMOTE_ADDRESS:
            case REQUEST_URI:
            case USER_ROLE:
            case USER_PRINCIPAL:
            case SESSION_ID:
                return true;
            default:
                return false;
        }
    }
}
