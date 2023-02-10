package io.github.poshjosh.ratelimiter.web.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UrlPathHelperTest {

    private HttpServletRequest request;
    private String encoding;
    private String contextPath;
    private String appPath;
    private String servletPath;

    @BeforeEach
    void beforeEach() {
        encoding = "ISO-8859-1";
        contextPath = "/context-path";
        appPath = "/app-path";
        servletPath = "/servlet-path";
        request = mock(HttpServletRequest.class);
        when(request.getContextPath()).then(invocationOnMock -> contextPath);
        when(request.getServletPath()).then(invocationOnMock -> servletPath);
        when(request.getPathInfo()).then(invocationOnMock -> null);
        when(request.getRequestURI()).then(invocationOnMock -> contextPath + pathWithinApplication());
        when(request.getCharacterEncoding()).then(invocationOnMock -> encoding);
    }

    private String pathWithinServlet() {
        return servletPath + "/path-info";
    }

    private String pathWithinApplication() {
        return appPath + pathWithinServlet();
    }

    @Test
    void getPathWithinApplication_givenNonEmptyContextPath_shouldReturnPathWithoutContextPath() {
        getPathWithinApplication_shouldReturnPathWithoutContextPath();
    }

    @Test
    void getPathWithinApplication_givenEmptyContextPath_shouldReturnPathWithoutContextPath() {
        contextPath = "";
        getPathWithinApplication_shouldReturnPathWithoutContextPath();
    }

    @Test
    void getPathWithinApplication_givenEmptyAppPath_shouldReturnPathWithoutContextPath() {
        appPath = "";
        getPathWithinApplication_shouldReturnPathWithoutContextPath();
    }

    @Test
    void getPathWithinApplication_givenEmptyServletPath_shouldReturnPathWithoutContextPath() {
        servletPath = "";
        getPathWithinApplication_shouldReturnPathWithoutContextPath();
    }

    @Test
    void getPathWithinApplication_givenEmptyPaths_shouldReturnPathWithoutContextPath() {
        contextPath = "";
        appPath = "";
        servletPath = "";
        getPathWithinApplication_shouldReturnPathWithoutContextPath();
    }

    private void getPathWithinApplication_shouldReturnPathWithoutContextPath() {
        String pathWithinApplication = getUrlPathHelper().getPathWithinApplication(request);
        assertEquals(pathWithinApplication(), pathWithinApplication);
    }

    @Test
    void getPathWithinServlet_givenNonEmptyServletPath_shouldReturnPathWithoutContextPathOrServletPath() {
        getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath();
    }

    @Test
    void getPathWithinServlet_givenEmptyContextPath_shouldReturnPathWithoutContextPathOrServletPath() {
        contextPath = "";
        getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath();
    }

    @Test
    void getPathWithinServlet_givenEmptyAppPath_shouldReturnPathWithoutContextPathOrServletPath() {
        appPath = "";
        getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath();
    }

    @Test
    void getPathWithinServlet_givenEmptyServletPath_shouldReturnPathWithoutContextPathOrServletPath() {
        servletPath = "";
        getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath();
    }

    @Test
    void getPathWithinServlet_givenEmptyPaths_shouldReturnPathWithoutContextPathOrServletPath() {
        contextPath = "";
        appPath = "";
        servletPath = "";
        getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath();
    }

    private void getPathWithinServlet_shouldReturnPathWithoutContextPathOrServletPath() {
        String pathWithinServlet = getUrlPathHelper().getPathWithinServlet(request);
        assertEquals(pathWithinServlet(), pathWithinServlet);
    }

    @Test
    void shouldDecode() {
        appPath = appPath + "%3F";
        String pathWithinApp = getUrlPathHelper().getPathWithinApplication(request);
        assertNotEquals(pathWithinApplication(), pathWithinApp);
        assertEquals(pathWithinApplication().replace("%3F", "?"), pathWithinApp);
    }

    private UrlPathHelper getUrlPathHelper() {
        return new UrlPathHelper(appPath);
    }
}