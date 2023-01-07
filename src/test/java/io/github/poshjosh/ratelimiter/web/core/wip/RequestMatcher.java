package io.github.poshjosh.ratelimiter.web.core.wip;

import io.github.poshjosh.ratelimiter.util.Matcher;

import java.security.Principal;
import java.util.*;

public interface RequestMatcher<R> {

    interface RequestInfo {
        interface Cookie{
            static Cookie of(String name, String value) {
                Objects.requireNonNull(name);
                Objects.requireNonNull(value);
                return new Cookie() {
                    @Override public String name() { return name ; }
                    @Override public String value() { return value; }
                    @Override public int hashCode() {
                        return Objects.hash(name(), value());
                    }
                    @Override public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        return name().equals(((RequestInfo.Cookie)o).name()) && value().equals(((RequestInfo.Cookie)o).value());
                    }
                    @Override public String toString() {
                        return "RequestInfo%Cookie{" + name + "=" + value + "}";
                    }
                };
            }
            String name();
            String value();
        }

        String getAuthType();

        Cookie[] getCookies();

        String getHeader(String name);

        Object getAttribute(String name);

        String [] getParameterValues(String name);

        String getRemoteAddr();

        Locale[] getLocales();

        boolean isUserInRole(String role);

        Principal getUserPrincipal();

        String getRequestURI();

        String getSessionId();

        default boolean containsCookie(String name) {
            return Arrays.stream(getCookies()).anyMatch(cookie -> Objects.equals(name, cookie.name()));
        }

        default boolean containsCookie(String name, String value) {
            return Arrays.asList(getCookies()).contains(Cookie.of(name, value));
        }
    }

    default Matcher<R, String> ofAuthType(String authType) {
        return target -> {
            if (Objects.equals(authType, getInfo(target).getAuthType())) {
                return authType;
            }
            return null;
        };
    }

    default Matcher<R, String> ofCookie(String name, String value) {
        return request -> {
            final RequestInfo info = getInfo(request);
            if (value.isEmpty() && info.containsCookie(name)) {
                return name;
            }
            if (!value.isEmpty() && info.containsCookie(name, value)) {
                return name + "=" + value;
            }
            return null;
        };
    }

    default Matcher<R, String> ofHeader(String name, String value) {
        return request -> {
            final RequestInfo info = getInfo(request);
            final String expected = info.getHeader(name);
            if (value.isEmpty() && expected != null && !expected.isEmpty()) {
                return name;
            }
            if (!value.isEmpty() && Objects.equals(value, expected)) {
                return name + "=" + value;
            }
            return null;
        };
    }

    default Matcher<R, String> ofAttribute(String name, String value) {
        return request -> {
            final RequestInfo info = getInfo(request);
            final Object expected = info.getAttribute(name);
            if (value.isEmpty() && expected != null) {
                return name;
            }
            if (!value.isEmpty() && Objects.equals(value, expected)) {
                return name + "=" + value;
            }
            return null;
        };
    }

    default Matcher<R, String> ofParameter(String name, String... values) {
        return request -> {
            final RequestInfo info = getInfo(request);
            final String[] expected = info.getParameterValues(name);
            if ((values == null || values.length == 0) && expected != null && expected.length > 0) {
                return name;
            }
            if ((values != null && values.length != 0) && Arrays.equals(values, expected)) {
                return name + "=" + Arrays.toString(values);
            }
            return null;
        };
    }

    default Matcher<R, String> ofRemoteAddr(String remoteAddress) {
        return target -> {
            if (Objects.equals(remoteAddress, getInfo(target).getRemoteAddr())) {
                return remoteAddress;
            }
            return null;
        };
    }

    /**
     * @param languageTags tags of format en-US, de-DE etc
     * @return A {@link Matcher} that will match the specified locale language tags
     */
    default Matcher<R, String> ofLocales(String... languageTags) {
        Object [] locales = Arrays.stream(languageTags)
                .map(locale -> locale.replace('_', '-'))
                .map(Locale::forLanguageTag).toArray();
        return target -> {
            if (Arrays.equals(locales, getInfo(target).getLocales())) {
                return Arrays.toString(languageTags);
            }
            return null;
        };
    }

    default Matcher<R, String> ofUserRole(String role) {
        return target -> {
            if (getInfo(target).isUserInRole(role)) {
                return role;
            }
            return null;
        };
    }

    default Matcher<R, String> ofUserPrincipal(String userPrincipalName) {
        return target -> {
            if (Objects.equals(userPrincipalName, getInfo(target).getUserPrincipal().getName())) {
                return userPrincipalName;
            }
            return null;
        };
    }

    default Matcher<R, String> ofRequestUri(String requestUri) {
        return target -> {
            if (Objects.equals(requestUri, getInfo(target).getRequestURI())) {
                return requestUri;
            }
            return null;
        };
    }

    default Matcher<R, String> ofSessionId(String sessionId) {
        return target -> {
            if (Objects.equals(sessionId, getInfo(target).getSessionId())) {
                return sessionId;
            }
            return null;
        };
    }

    RequestInfo getInfo(R request);
}
