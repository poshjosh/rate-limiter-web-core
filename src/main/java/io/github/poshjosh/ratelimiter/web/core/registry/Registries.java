package io.github.poshjosh.ratelimiter.web.core.registry;

import io.github.poshjosh.ratelimiter.util.Matcher;
import javax.servlet.http.HttpServletRequest;

public interface Registries extends UnmodifiableRegistries {

    static Registries ofDefaults() {
        return of(Matcher.matchNone());
    }

    static Registries of(Matcher<HttpServletRequest> matcher) {
        return new DefaultRegistries(matcher);
    }

    static UnmodifiableRegistries unmodifiable(Registries registries) {
        return new UnmodifiableRegistries() {
            @Override public UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers() {
                return Registry.unmodifiable(registries.matchers());
            }
            @Override public String toString() { return "Unmodifiable{" + registries + "}"; }
        };
    }

    Registry<Matcher<HttpServletRequest>> matchers();
}