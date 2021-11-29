package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateFactory;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface RateLimiterConfigurationRegistry<R> {

    void registerRequestMatcher(Class<?> clazz, Matcher<R> matcher);

    void registerRequestMatcher(Method method, Matcher<R> matcher);

    void registerRequestMatcher(String name, Matcher<R> matcher);

    void registerRateCache(RateCache<Object> rateCache);

    void registerRateCache(Class<?> clazz, RateCache<Object> rateCache);

    void registerRateCache(Method method, RateCache<Object> rateCache);

    void registerRateCache(String name, RateCache<Object> rateCache);

    void registerRateFactory(RateFactory rateFactory);

    void registerRateFactory(Class<?> clazz, RateFactory rateFactory);

    void registerRateFactory(Method method, RateFactory rateFactory);

    void registerRateFactory(String name, RateFactory rateFactory);

    void registerRateRecordedListener(RateRecordedListener rateRecordedListener);

    void registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener);

    void registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener);

    void registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener);

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    void registerRootRateRecordedListener(RateRecordedListener rateRecordedListener);

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    void addRootRateRecordedListener(RateRecordedListener rateRecordedListener);
}
