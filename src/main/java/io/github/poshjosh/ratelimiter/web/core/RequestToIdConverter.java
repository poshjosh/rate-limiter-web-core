package io.github.poshjosh.ratelimiter.web.core;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface RequestToIdConverter<R, K> {

    static RequestToIdConverter<HttpServletRequest, String> ofHttpServletRequest() {
        return new DefaultRequestToIdConverter();
    }

    K toId(R request);
}
