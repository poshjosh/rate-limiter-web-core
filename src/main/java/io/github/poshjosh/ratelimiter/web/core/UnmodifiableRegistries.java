package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Matcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface UnmodifiableRegistries {

    UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers();

    default BandwidthsStore<?> getStoreOrDefault() {
        return getStore().orElse(BandwidthsStore.ofDefaults());
    }

    Optional<BandwidthsStore<?>> getStore();

    default UsageListener getListenerOrDefault() {
        return getListener().orElse(UsageListener.NO_OP);
    }

    Optional<UsageListener> getListener();
}