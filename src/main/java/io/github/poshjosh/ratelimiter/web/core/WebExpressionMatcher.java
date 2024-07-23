package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.*;
import javax.servlet.http.HttpServletRequest;

public interface WebExpressionMatcher
        extends ExpressionMatcher<HttpServletRequest>, WebExpressionKey{

    static WebExpressionMatcher ofHttpServletRequest() {
        return new DefaultWebExpressionMatcher();
    }
}
