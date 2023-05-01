package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

final class DefaultRegistries implements Registries {

    private final Registry<Matcher<HttpServletRequest>> matcherRegistry;

    private final Registry<UsageListener> listenerRegistry;

    DefaultRegistries(Matcher<HttpServletRequest> matcher, UsageListener listener) {
        this.matcherRegistry = Registry.of(matcher);
        this.listenerRegistry = Registry.of(listener);
    }

    @Override
    public Registry<Matcher<HttpServletRequest>> matchers() {
        return matcherRegistry;
    }

    @Override public Registry<UsageListener> listeners() { return listenerRegistry; }
}
