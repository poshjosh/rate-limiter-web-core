package io.github.poshjosh.ratelimiter.web.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

final class DefaultRequestInfo implements RequestInfo {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestInfo.class);

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
        return getClientIpAddress(request, "");
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

    private static final String[] IP_ADDR_RELATED_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };

    private String getClientIpAddress(ServletRequest request, String resultIfNone) {
        String ip = null;
        try {
            if(request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest)request;
                for (String header : IP_ADDR_RELATED_HEADERS) {
                    ip = httpServletRequest.getHeader(header);
                    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                        if (ip.contains(",")) {
                            ip = ip.split(",")[0];
                        }
                        break;
                    }
                }
            }
        }catch(Exception e) {
            LOG.warn("Error resolving ip address", e);
        }
        if(ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip == null ? resultIfNone : ip;
    }
}
