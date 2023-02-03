package io.github.poshjosh.ratelimiter.web.core;

import javax.servlet.http.HttpServletRequest;

final class DefaultRequestToIdConverter implements RequestToIdConverter<HttpServletRequest, String>{

    DefaultRequestToIdConverter() { }

    @Override public String toId(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
