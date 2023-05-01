package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

public interface Registries extends UnmodifiableRegistries {

    static Registries ofDefaults() {
        return of(Matcher.matchNone(), UsageListener.NO_OP);
    }

    static Registries of(Matcher<HttpServletRequest> matcher, UsageListener listener) {
        return new DefaultRegistries(matcher, listener);
    }

    static UnmodifiableRegistries unmodifiable(Registries registries) {
        return new UnmodifiableRegistries() {
            @Override public UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers() {
                return Registry.unmodifiable(registries.matchers());
            }
            @Override public UnmodifiableRegistry<UsageListener> listeners() {
                return Registry.unmodifiable(registries.listeners());
            }
            @Override public String toString() { return "Unmodifiable{" + registries + "}"; }
        };
    }

    Registry<Matcher<HttpServletRequest>> matchers();

    Registry<UsageListener> listeners();
}