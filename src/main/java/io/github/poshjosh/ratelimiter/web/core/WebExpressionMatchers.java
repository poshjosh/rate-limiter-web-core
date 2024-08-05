package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.*;
import javax.servlet.http.HttpServletRequest;

public interface WebExpressionMatchers {
    static ExpressionMatcher<HttpServletRequest> ofHttpServletRequest() {
        return new HttpRequestExpressionMatcher();
    }
}
