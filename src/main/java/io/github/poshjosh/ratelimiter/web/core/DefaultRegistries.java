package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;

import java.util.Optional;

final class DefaultRegistries<R> implements Registries<R> {

    private final Registry<Matcher<R>> matcherRegistry;

    private BandwidthsStore<?> store;

    private UsageListener listener;

    DefaultRegistries(Matcher<R> matcher) {
        this.matcherRegistry = Registry.of(matcher);
    }

    @Override
    public Registry<Matcher<R>> matchers() {
        return matcherRegistry;
    }

    @Override
    public Optional<BandwidthsStore<?>> getStore() {
        return Optional.ofNullable(store);
    }

    @Override
    public Registries<R> registerStore(BandwidthsStore<?> store) {
        this.store = store;
        return this;
    }

    @Override
    public Optional<UsageListener> getListener() {
        return Optional.ofNullable(listener);
    }

    @Override
    public Registries<R> registerListener(UsageListener listener) {
        this.listener = listener;
        return this;
    }
}
