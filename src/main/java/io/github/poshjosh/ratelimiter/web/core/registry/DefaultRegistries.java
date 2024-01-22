package io.github.poshjosh.ratelimiter.web.core.registry;

import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

final class DefaultRegistries implements Registries {

    private final Registry<Matcher<HttpServletRequest>> matcherRegistry;

    DefaultRegistries(Matcher<HttpServletRequest> matcher) {
        this.matcherRegistry = Registry.of(matcher);
    }

    @Override
    public Registry<Matcher<HttpServletRequest>> matchers() {
        return matcherRegistry;
    }
}
