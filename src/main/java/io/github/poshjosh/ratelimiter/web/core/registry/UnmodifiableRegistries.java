package io.github.poshjosh.ratelimiter.web.core.registry;

import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

public interface UnmodifiableRegistries {
    UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers();
}