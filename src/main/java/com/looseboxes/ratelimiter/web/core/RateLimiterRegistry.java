package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiterConfig;
import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateFactory;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface RateLimiterRegistry<R> extends MatcherRegistry<R>{

    @Override RateLimiterRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher);

    @Override RateLimiterRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher);

    @Override RateLimiterRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher);

    RateLimiterConfig<?, ?> getRateLimiterConfig(String name);

    RateLimiterRegistry<R> registerRateCache(RateCache<?, ?> rateCache);

    RateLimiterRegistry<R> registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache);

    RateLimiterRegistry<R> registerRateCache(Method method, RateCache<?, ?> rateCache);

    RateLimiterRegistry<R> registerRateCache(String name, RateCache<?, ?> rateCache);

    RateLimiterRegistry<R> registerRateFactory(RateFactory rateFactory);

    RateLimiterRegistry<R> registerRateFactory(Class<?> clazz, RateFactory rateFactory);

    RateLimiterRegistry<R> registerRateFactory(Method method, RateFactory rateFactory);

    RateLimiterRegistry<R> registerRateFactory(String name, RateFactory rateFactory);

    RateLimiterRegistry<R> registerRateRecordedListener(RateRecordedListener rateRecordedListener);

    RateLimiterRegistry<R> registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener);

    RateLimiterRegistry<R> registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener);

    RateLimiterRegistry<R> registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener);

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    RateLimiterRegistry<R> registerRootRateRecordedListener(RateRecordedListener rateRecordedListener);

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    RateLimiterRegistry<R> addRootRateRecordedListener(RateRecordedListener rateRecordedListener);

    RateLimiterRegistry<R> registerRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterRegistry<R> registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterRegistry<R> registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterRegistry<R> registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterFactory<?> getRateLimiterFactory(String name);
}
