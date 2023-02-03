package io.github.poshjosh.ratelimiter.web.core;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

final class DefaultRequestInfo implements RequestInfo {
    private final HttpServletRequest request;
    DefaultRequestInfo(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request);
    }
    @Override public String getAuthScheme() {
        String authScheme = request.getAuthType();
        return authScheme == null ? "" : authScheme;
    }
    @Override public List<Cookie> getCookies() {
        javax.servlet.http.Cookie [] cookies = request.getCookies();
        return cookies == null || cookies.length == 0 ? Collections.emptyList() :
                Arrays.stream(request.getCookies())
                        .map(cookie -> Cookie.of(cookie.getName(), cookie.getValue()))
                        .collect(Collectors.toList());

    }
    @Override public List<String> getHeaders(String name) {
        Enumeration<String> headers = request.getHeaders(name);
        return headers == null ? Collections.emptyList() :
                Collections.list(request.getHeaders(name));
    }
    @Override public Object getAttribute(String name) {
        return request.getAttribute(name);
    }
    @Override public List<String> getParameters(String name) {
        String [] values = request.getParameterValues(name);
        return values == null || values.length == 0
                ? Collections.emptyList() : Arrays.asList(values);
    }
    @Override public String getRemoteAddr() {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr;
    }
    @Override public List<Locale> getLocales() {
        Enumeration<Locale> locales = request.getLocales();
        return locales == null ? Collections.emptyList() : Collections.list(locales);
    }
    @Override public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }
    @Override public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }
    @Override public String getRequestUri() {
        return request.getRequestURI();
    }
    @Override public String getSessionId() {
        final String id = request.getSession(true).getId();
        return id == null ? "" : id;
    }
}
