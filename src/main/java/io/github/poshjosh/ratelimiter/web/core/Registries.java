package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.util.Matcher;

import javax.cache.Cache;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface Registries {

    static Registries ofDefaults() {
        return of(Matcher.matchNone());
    }

    static Registries of(Matcher<HttpServletRequest> matcher) {
        return new DefaultRegistries(matcher);
    }

    Registry<Matcher<HttpServletRequest>> matchers();

    default BandwidthsStore<?> getStoreOrDefault() {
        return getStore().orElse(BandwidthsStore.ofDefaults());
    }

    Optional<BandwidthsStore<?>> getStore();

    default Registries registerCache(Cache<?, Bandwidth> cache) {
        return registerStore(BandwidthsStore.ofCache(cache));
    }

    Registries registerStore(BandwidthsStore<?> store);



    default UsageListener getListenerOrDefault() {
        return getListener().orElse(UsageListener.NO_OP);
    }

    Optional<UsageListener> getListener();

    default Registries addListener(UsageListener listener) {
        registerListener(getListener().map(existing -> existing.andThen(listener)).orElse(listener));
        return this;
    }

    Registries registerListener(UsageListener listener);
}