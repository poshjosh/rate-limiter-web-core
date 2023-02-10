package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

final class DefaultRegistries implements Registries {

    private final Registry<Matcher<HttpServletRequest>> matcherRegistry;

    private BandwidthsStore<?> store;

    private UsageListener listener;

    DefaultRegistries(Matcher<HttpServletRequest> matcher) {
        this.matcherRegistry = Registry.of(matcher);
    }

    @Override
    public Registry<Matcher<HttpServletRequest>> matchers() {
        return matcherRegistry;
    }

    @Override
    public Optional<BandwidthsStore<?>> getStore() {
        return Optional.ofNullable(store);
    }

    @Override
    public Registries registerStore(BandwidthsStore<?> store) {
        this.store = store;
        return this;
    }

    @Override
    public Optional<UsageListener> getListener() {
        return Optional.ofNullable(listener);
    }

    @Override
    public Registries registerListener(UsageListener listener) {
        this.listener = listener;
        return this;
    }
}
