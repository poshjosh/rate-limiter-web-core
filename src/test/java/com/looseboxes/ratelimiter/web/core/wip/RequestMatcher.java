package com.looseboxes.ratelimiter.web.core.wip;

import com.looseboxes.ratelimiter.util.Matcher;

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

        //String getRequestURI();

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
            if ((values == null || values.length == 0) && expected != null) {
                return name;
            }
            if ((values != null && values.length != 0) && Arrays.equals(values, expected)) {
                return name + "=" + Arrays.toString(values);
            }
            return null;
        };
    }

    RequestInfo getInfo(R request);
}
