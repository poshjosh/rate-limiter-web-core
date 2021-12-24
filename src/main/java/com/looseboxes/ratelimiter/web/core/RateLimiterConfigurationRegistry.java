package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateFactory;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface RateLimiterConfigurationRegistry<R> {

    void registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher);

    void registerRequestMatcher(Method method, Matcher<R, ?> matcher);

    void registerRequestMatcher(String name, Matcher<R, ?> matcher);

    void registerRateCache(RateCache<?, ?> rateCache);

    void registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache);

    void registerRateCache(Method method, RateCache<?, ?> rateCache);

    void registerRateCache(String name, RateCache<?, ?> rateCache);

    void registerRateFactory(RateFactory rateFactory);

    void registerRateFactory(Class<?> clazz, RateFactory rateFactory);

    void registerRateFactory(Method method, RateFactory rateFactory);

    void registerRateFactory(String name, RateFactory rateFactory);

    void registerRateExceededListener(RateRecordedListener rateRecordedListener);

    void registerRateExceededListener(Class<?> clazz, RateRecordedListener rateRecordedListener);

    void registerRateExceededListener(Method method, RateRecordedListener rateRecordedListener);

    void registerRateExceededListener(String name, RateRecordedListener rateRecordedListener);

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    void registerRootRateExceededListener(RateRecordedListener rateRecordedListener);

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    void addRootRateExceededListener(RateRecordedListener rateRecordedListener);

    void registerDefaultRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory);

    void registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory);

    void registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory);

    void registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory);
}
