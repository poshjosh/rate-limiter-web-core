package io.github.poshjosh.ratelimiter.web.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

final class DefaultRequestInfo {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestInfo.class);
    static String authScheme(HttpServletRequest request, String resultIfNone) {
        String authScheme = request.getAuthType();
        return authScheme == null ? resultIfNone : authScheme;
    }
    static List<Cookie> cookies(HttpServletRequest request) {
        javax.servlet.http.Cookie [] cookies = request.getCookies();
        return cookies == null || cookies.length == 0 ?
                Collections.emptyList() : Arrays.asList(cookies);

    }
    static List<String> headers(HttpServletRequest request, String name) {
        Enumeration<String> headers = request.getHeaders(name);
        return headers == null ? Collections.emptyList() :
                Collections.list(request.getHeaders(name));
    }
    static List<String> parameters(HttpServletRequest request, String name) {
        String [] values = request.getParameterValues(name);
        return values == null || values.length == 0
                ? Collections.emptyList() : Arrays.asList(values);
    }
    static String remoteAddr(HttpServletRequest request, String resultIfNone) {
        return getClientIpAddress(request, resultIfNone);
    }
    static List<Locale> locales(HttpServletRequest request) {
        Enumeration<Locale> locales = request.getLocales();
        return locales == null ? Collections.emptyList() : Collections.list(locales);
    }
    static String sessionId(HttpServletRequest request, String resultIfNone) {
        final String id = request.getSession(true).getId();
        return id == null ? resultIfNone : id;
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

    private DefaultRequestInfo() { }
}
