package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.web.core.util.MatchConfig;
import io.github.poshjosh.ratelimiter.web.core.util.MatchType;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.poshjosh.ratelimiter.web.core.MatchUtils.matchOrNull;

public interface RequestMatcherFactory<R> extends RequestToIdConverter<R, String>{

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
                        return "RequestInfo$Cookie{" + name + "=" + value + "}";
                    }
                };
            }
            String name();
            String value();
        }

        String getAuthScheme();

        List<Cookie> getCookies();

        List<String> getHeaders(String name);

        Object getAttribute(String name);

        List<String> getParameters(String name);

        String getRemoteAddr();

        List<Locale> getLocales();

        boolean isUserInRole(String role);

        Principal getUserPrincipal();

        String getRequestUri();

        String getSessionId();
    }

    default Optional<Matcher<R, ?>> of(MatchConfig matchConfig) {
        return Optional.ofNullable(of(matchConfig, null));
    }

    default Matcher<R, ?> of(MatchConfig matchConfig, Matcher<R, ?> resultIfNone) {
        final MatchType matchType = matchConfig.getMatchType();
        final Operator operator = matchConfig.getOperator();
        final String key = matchConfig.getName();
        final String [] vals = matchConfig.getValues();

        switch(matchType) {
            case NO_OP: return resultIfNone;

            case ATTRIBUTE: return req -> matchOrNull(operator, key, info(req).getAttribute(key), vals);

            case AUTH_SCHEME: return req -> matchOrNull(operator, info(req).getAuthScheme(), vals);

            case COOKIE:
                RequestInfo.Cookie [] cookies = (RequestInfo.Cookie[])Arrays.stream(vals)
                        .map(cookieValue -> RequestInfo.Cookie.of(key, cookieValue))
                        .toArray();
                return req -> matchOrNull(operator, key, info(req).getCookies(), cookies);

            case HEADER: return req -> matchOrNull(operator, key, info(req).getHeaders(key), vals);

            case LOCALE:
                Locale [] locales = Arrays.stream(vals)
                        .map(locale -> locale.replace('_', '-'))
                        .map(Locale::forLanguageTag)
                        .collect(Collectors.toList()).toArray(new Locale[0]);
                return req -> matchOrNull(operator, info(req).getLocales(), locales);

            case PARAMETER: return req -> matchOrNull(operator, key, info(req).getParameters(key), vals);

            case REMOTE_ADDRESS: return req -> matchOrNull(operator, info(req).getRemoteAddr(), vals);

            case REQUEST_URI: return req -> matchOrNull(operator, info(req).getRequestUri(), vals);

            case USER_PRINCIPAL: return req -> matchOrNull(operator, info(req).getUserPrincipal().getName(), vals);

            case USER_ROLE:
                return req -> {
                    if (vals.length == 0) {
                        return null;
                    }
                    RequestInfo info = info(req);
                    boolean anyMatch = Operator.OR.equals(operator);
                    for (String role : vals) {
                        boolean userInRole = info.isUserInRole(role);
                        if (anyMatch && userInRole) {
                            return MatchUtils.toString(vals);
                        }
                        if (!anyMatch && !userInRole) {
                            return null;
                        }
                    }
                    return anyMatch ? null : MatchUtils.toString(vals);
                };

            case SESSION_ID: return req -> matchOrNull(operator, info(req).getSessionId(), vals);

            default: throw new UnsupportedOperationException("Unexpected " +
                    MatchType.class.getName() + ": " + matchType);
        }
    }

    RequestInfo info(R request);
}
