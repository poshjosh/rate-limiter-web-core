package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface Registries extends UnmodifiableRegistries{

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
            @Override public Optional<BandwidthsStore<?>> getStore() {
                return registries.getStore();
            }
            @Override public Optional<UsageListener> getListener() {
                return registries.getListener();
            }
            @Override public String toString() { return "Unmodifiable{" + registries + "}"; }
        };
    }

    Registry<Matcher<HttpServletRequest>> matchers();

    Registries registerStore(BandwidthsStore<?> store);

    default Registries addListener(UsageListener listener) {
        registerListener(getListener().map(existing -> existing.andThen(listener)).orElse(listener));
        return this;
    }

    Registries registerListener(UsageListener listener);
}