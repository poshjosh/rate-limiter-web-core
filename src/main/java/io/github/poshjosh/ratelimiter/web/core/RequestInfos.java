package io.github.poshjosh.ratelimiter.web.core;

import javax.servlet.http.HttpServletRequest;

public interface RequestInfos {
    static RequestInfo of(HttpServletRequest request) {
        return new DefaultRequestInfo(request);
    }
}
