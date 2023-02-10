package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

/**
 * Helper class for URL path matching. Provides support for consistent URL decoding.
 *
 * Adapted from org.springframework.web.util.UrlPathHelper
 * <p>We need this class for the following reasons:</p>
 * <ul>
 *     <li>HttpServletRequest#getPathInfo() may return null, e.g within Filters.</li>
 *     <li>HttpServletRequest#getRequestURI() is not decoded</li>
 * </ul>
 * <p>For reference, the various path related methods of HttpServletRequest are shown below:</p>
 * Application deployed under: /app
 * <br/>Servlet is mapped as: /test%3F/*
 * <br/>URL: http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a
 *
 * <pre>
 * Method              URL-Decoded Result
 * ----------------------------------------------------
 * getContextPath()        no      /app
 * getLocalAddr()                  127.0.0.1
 * getLocalName()                  30thh.loc
 * getLocalPort()                  8480
 * getMethod()                     GET
 * getPathInfo()           yes     /a?+b
 * getProtocol()                   HTTP/1.1
 * getQueryString()        no      p+1=c+d&p+2=e+f
 * getRequestedSessionId() no      S%3F+ID
 * getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
 * getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
 * getScheme()                     http
 * getServerName()                 30thh.loc
 * getServerPort()                 8480
 * getServletPath()        yes     /test?
 * getParameterNames()     yes     [p 2, p 1]
 * getParameter("p 1")     yes     c d
 * </pre>
 */
final class UrlPathHelper {

    static final String DEFAULT_ENCODING = "ISO-8859-1";

    private static final Logger LOG = LoggerFactory.getLogger(UrlPathHelper.class);

    private final String applicationPath;

    UrlPathHelper(String applicationPath) {
        this.applicationPath = Objects.requireNonNull(applicationPath);
        if (applicationPath.contains("*") || applicationPath.contains("?")) {
            // Tag:Issue:Application-paths-containing-asterix-or-question-mark-not-supported
            throw new UnsupportedOperationException(
                    "Application paths with * or ? are not currently supported");
        }
    }

    /**
     * Get the path beginning with the servletPath.
     *
     * If the servletPath is an empty string, this means the servlet used to process this request
     * was matched using the "/*" pattern. See {@link HttpServletRequest#getServletPath()}.
     * @param request The HttpServletRequest whose path will be returned
     * @return the path beginning with the servletPath.
     */
    public String getPathWithinServlet(HttpServletRequest request) {
        final String path = getPathWithinApplication(request);
        final String servletPath = request.getServletPath();
        final String pathWithinServlet;
        if (StringUtils.hasText(applicationPath)) {
            final int index = path.indexOf(applicationPath);
            if (index == -1) {
                throw new IllegalArgumentException("Path " + path +
                        ", does not contain the provided application path: " + applicationPath);
            }
            pathWithinServlet = path.substring(index + applicationPath.length());
        } else if (!servletPath.isEmpty()) {
            final int index = path.indexOf(servletPath);
            if (index == -1) {
                throw new IllegalArgumentException("Path " + path +
                        ", does not contain the servlet path: " + servletPath);
            }
            pathWithinServlet = path.substring(index);
        } else {
            pathWithinServlet = path;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Path within servlet: {}, request URI: {}",
                    pathWithinServlet, request.getRequestURI());
        }
        return pathWithinServlet;
    }

    /**
     * Return the path within the web application for the given request.
     * <p>Detects include request URL if called within a RequestDispatcher include.
     * @param request current HTTP request
     * @return the path within the web application
     */
    public String getPathWithinApplication(HttpServletRequest request) {
        String contextPath = getContextPath(request);
        String requestUri = getRequestUri(request);
        String path = getRemainingPath(requestUri, contextPath, true);
        if (path != null) {
            // Normal case: URI contains context path.
            return (StringUtils.hasText(path) ? path : "/");
        }
        else {
            return requestUri;
        }
    }

    /**
     * Match the given "mapping" to the start of the "requestUri" and if there
     * is a match return the extra part. This method is needed because the
     * context path and the servlet path returned by the HttpServletRequest are
     * stripped of semicolon content unlike the requestUri.
     */
    private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
        int index1 = 0;
        int index2 = 0;
        for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
            char c1 = requestUri.charAt(index1);
            char c2 = mapping.charAt(index2);
            if (c1 == ';') {
                index1 = requestUri.indexOf('/', index1);
                if (index1 == -1) {
                    return null;
                }
                c1 = requestUri.charAt(index1);
            }
            if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
                continue;
            }
            return null;
        }
        if (index2 != mapping.length()) {
            return null;
        }
        else if (index1 == requestUri.length()) {
            return "";
        }
        else if (requestUri.charAt(index1) == ';') {
            index1 = requestUri.indexOf('/', index1);
        }
        return (index1 != -1 ? requestUri.substring(index1) : "");
    }

    /**
     * Sanitize the given path. Uses the following rules:
     * <ul>
     * <li>replace all "//" by "/"</li>
     * </ul>
     */
    private String getSanitizedPath(final String path) {
        String sanitized = path;
        while (true) {
            int index = sanitized.indexOf("//");
            if (index < 0) {
                break;
            }
            else {
                sanitized = sanitized.substring(0, index) + sanitized.substring(index + 1);
            }
        }
        return sanitized;
    }

    /**
     * Return the request URI for the given request.
     * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
     * decoded by the servlet container, this method will decode it.
     * <p>The URI that the web container resolves <i>should</i> be correct, but some
     * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
     * in the URI. This method cuts off such incorrect appendices.
     * @param request current HTTP request
     * @return the request URI
     */
    private String getRequestUri(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        return decodeAndCleanUriString(request, uri);
    }

    /**
     * Return the context path for the given request, detecting an include request
     * URL if called within a RequestDispatcher include.
     * <p>As the value returned by {@code request.getContextPath()} is <i>not</i>
     * decoded by the servlet container, this method will decode it.
     * @param request current HTTP request
     * @return the context path
     */
    private String getContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if ("/".equals(contextPath)) {
            // Invalid case, but happens for includes on Jetty: silently adapt it.
            contextPath = "";
        }
        return decodeRequestString(request, contextPath);
    }

    /**
     * Decode the supplied URI string and strips any extraneous portion after a ';'.
     */
    private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
        uri = removeSemicolonContent(uri);
        uri = decodeRequestString(request, uri);
        uri = getSanitizedPath(uri);
        return uri;
    }

    /**
     * Decode the given source string with a URLDecoder. The encoding will be taken
     * from the request, falling back to the default "ISO-8859-1".
     * <p>The default implementation uses {@code URLDecoder.decode(input, enc)}.
     * @param request current HTTP request
     * @param source the String to decode
     * @return the decoded String
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     * @see URLDecoder#decode(String, String)
     * @see URLDecoder#decode(String)
     */
    private String decodeRequestString(HttpServletRequest request, String source) {
        String enc = determineEncoding(request);
        try {
            return URLDecoder.decode(source, enc);
        }catch (UnsupportedEncodingException ex) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not decode request string [{}] with encoding '{}': falling back to platform default encoding; exception message: {}",
                        source, enc, ex.getMessage());
            }
            return URLDecoder.decode(source);
        }
    }

    /**
     * Determine the encoding for the given request.
     * <p>The default implementation checks the request encoding,
     * falling back to ISO-8859-1.
     * @param request current HTTP request
     * @return the encoding for the request (never {@code null})
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    private String determineEncoding(HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        if (StringUtils.hasText(enc)) {
            return enc;
        }
        return DEFAULT_ENCODING;
    }

    /**
     * Remove ";" (semicolon) content from the given request URI if the
     * {@linkplain #removeSemicolonContent}
     * property is set to "true". Note that "jsessionid" is always removed.
     * @param requestUri the request URI string to remove ";" content from
     * @return the updated URI string
     */
    private String removeSemicolonContent(String requestUri) {
        int semicolonIndex = requestUri.indexOf(';');
        while (semicolonIndex != -1) {
            int slashIndex = requestUri.indexOf('/', semicolonIndex);
            String start = requestUri.substring(0, semicolonIndex);
            requestUri = (slashIndex != -1) ? start + requestUri.substring(slashIndex) : start;
            semicolonIndex = requestUri.indexOf(';', semicolonIndex);
        }
        return requestUri;
    }
}
