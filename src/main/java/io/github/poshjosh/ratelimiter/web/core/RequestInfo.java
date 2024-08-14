package io.github.poshjosh.ratelimiter.web.core;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public interface RequestInfo {
    interface Cookie {
        Cookie EMPTY = Cookie.of("", "");
        static Cookie of(String name, String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            return new Cookie() {
                @Override public String name() {
                    return name;
                }

                @Override public String value() {
                    return value;
                }

                @Override public int hashCode() {
                    return Objects.hash(name(), value());
                }

                @Override public boolean equals(Object o) {
                    if (this == o)
                        return true;
                    if (o == null || getClass() != o.getClass())
                        return false;
                    return name().equals(((Cookie) o).name()) && value()
                            .equals(((Cookie) o).value());
                }

                @Override public String toString() {
                    return "RequestInfo$Cookie{" + name + "=" + value + "}";
                }
            };
        }

        String name();

        String value();
    }

    String getAuthScheme(String resultIfNone);

    String getCharacterEncoding(String resultIfNone);

    String getContextPath();

    List<Cookie> getCookies();

    List<String> getHeaders(String name);

    Object getAttribute(String name, Object resultIfNone);

    List<String> getParameters(String name);

    String getRemoteAddr(String resultIfNone);

    List<Locale> getLocales();

    String getMethod();

    String getRequestUri();

    String getServletPath();

    String getSessionId(String resultIfNone);

    Principal getUserPrincipal(Principal resultIfNone);

    boolean isUserInRole(String role);
}
