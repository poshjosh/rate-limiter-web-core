package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

public interface UnmodifiableRegistries {
    UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers();
    UnmodifiableRegistry<UsageListener> listeners();
}