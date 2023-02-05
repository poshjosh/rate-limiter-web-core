package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;

import javax.cache.Cache;
import java.util.Optional;

public interface Registries<R> {

    static <R> Registries<R> ofDefaults() {
        return of(Matcher.matchNone());
    }

    static <R> Registries<R> of(Matcher<R> matcher) {
        return new DefaultRegistries<>(matcher);
    }

    Registry<Matcher<R>> matchers();

    default BandwidthsStore<?> getStoreOrDefault() {
        return getStore().orElse(BandwidthsStore.ofDefaults());
    }

    Optional<BandwidthsStore<?>> getStore();

    default Registries<R> registerCache(Cache<?, Bandwidth[]> cache) {
        return registerStore(BandwidthsStore.ofCache(cache));
    }

    Registries<R> registerStore(BandwidthsStore<?> store);

    default UsageListener getListenerOrDefault() {
        return getListener().orElse(UsageListener.NO_OP);
    }

    Optional<UsageListener> getListener();

    Registries<R> registerListener(UsageListener listener);
}