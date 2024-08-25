package io.github.poshjosh.ratelimiter.web.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

public final class RequestInfos {
    private static final Logger LOG = LoggerFactory.getLogger(RequestInfos.class);

    public static RequestInfo of(HttpServletRequest request) {
        return new JavaxServletRequest(request);
    }

    private static final class JavaxServletRequest implements RequestInfo {

        private final HttpServletRequest request;
        JavaxServletRequest(HttpServletRequest request) {
            this.request = Objects.requireNonNull(request);
        }

        @Override public String getAuthScheme(String resultIfNone) {
            String authScheme = request.getAuthType();
            return authScheme == null ? resultIfNone : authScheme;
        }
        @Override public String getCharacterEncoding(String resultIfNone) {
            String encoding = request.getCharacterEncoding();
            return encoding == null ? resultIfNone : encoding;
        }
        @Override public String getContextPath() { return request.getContextPath(); }
        @Override public List<Cookie> getCookies() {
            javax.servlet.http.Cookie [] cookies = request.getCookies();
            return cookies == null ? null :
                    Arrays.stream(request.getCookies())
                            .map(cookie -> Cookie.of(cookie.getName(), cookie.getValue()))
                            .collect(Collectors.toList());
        }
        @Override public List<String> getHeaders(String name) {
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames == null) {
                return null;
            }
            while(headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if(headerName.equalsIgnoreCase(name)) { // Case-insensitive
                    return Collections.list(request.getHeaders(headerName));
                }
            }
            return null;
        }
        @Override public Object getAttribute(String name, Object resultIfNone) {
            Object attribute = request.getAttribute(name);
            return attribute == null ? resultIfNone : attribute;
        }
        @Override public List<String> getParameters(String name) {
            String [] values = request.getParameterValues(name);
            return values == null ? null : Arrays.asList(values);
        }
        @Override public String getRemoteAddr(String resultIfNone) {
            return getClientIpAddress(request, resultIfNone);
        }
        @Override public List<Locale> getLocales() {
            Enumeration<Locale> locales = request.getLocales();
            return locales == null ? null : Collections.list(locales);
        }

        @Override public String getMethod() {
            return request.getMethod();
        }

        @Override public boolean isUserInRole(String role) {
            return request.isUserInRole(role);
        }
        @Override public Principal getUserPrincipal(Principal resultIfNone) {
            Principal principal = request.getUserPrincipal();
            return principal == null ? resultIfNone : principal;
        }
        @Override public String getRequestUri() {
            return request.getRequestURI();
        }
        @Override public String getServletPath() { return request.getServletPath(); }
        @Override public String getSessionId(String resultIfNone) {
            final String id = request.getSession(true).getId();
            return id == null ? resultIfNone : id;
        }
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

    private static String getClientIpAddress(ServletRequest request, String resultIfNone) {
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

    private RequestInfos() { }
}
