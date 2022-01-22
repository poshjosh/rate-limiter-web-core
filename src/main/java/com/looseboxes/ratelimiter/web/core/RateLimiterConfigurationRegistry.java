package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateFactory;
import com.looseboxes.ratelimiter.RateLimiterFactory;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface RateLimiterConfigurationRegistry<R> extends MatcherRegistry<R>{

    @Override
    RateLimiterConfigurationRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher);

    @Override
    RateLimiterConfigurationRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher);

    @Override
    RateLimiterConfigurationRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher);

    RateLimiterConfigurationRegistry<R> registerRateCache(RateCache<?, ?> rateCache);

    RateLimiterConfigurationRegistry<R> registerRateCache(Class<?> clazz, RateCache<?, ?> rateCache);

    RateLimiterConfigurationRegistry<R> registerRateCache(Method method, RateCache<?, ?> rateCache);

    RateLimiterConfigurationRegistry<R> registerRateCache(String name, RateCache<?, ?> rateCache);

    RateLimiterConfigurationRegistry<R> registerRateFactory(RateFactory rateFactory);

    RateLimiterConfigurationRegistry<R> registerRateFactory(Class<?> clazz, RateFactory rateFactory);

    RateLimiterConfigurationRegistry<R> registerRateFactory(Method method, RateFactory rateFactory);

    RateLimiterConfigurationRegistry<R> registerRateFactory(String name, RateFactory rateFactory);

    RateLimiterConfigurationRegistry<R> registerRateRecordedListener(RateRecordedListener rateRecordedListener);

    RateLimiterConfigurationRegistry<R> registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener);

    RateLimiterConfigurationRegistry<R> registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener);

    RateLimiterConfigurationRegistry<R> registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener);

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    RateLimiterConfigurationRegistry<R> registerRootRateRecordedListener(RateRecordedListener rateRecordedListener);

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    RateLimiterConfigurationRegistry<R> addRootRateRecordedListener(RateRecordedListener rateRecordedListener);

    RateLimiterConfigurationRegistry<R> registerRateLimiterFactory(RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterConfigurationRegistry<R> registerRateLimiterFactory(Class<?> clazz, RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterConfigurationRegistry<R> registerRateLimiterFactory(Method method, RateLimiterFactory<?> rateLimiterFactory);

    RateLimiterConfigurationRegistry<R> registerRateLimiterFactory(String name, RateLimiterFactory<?> rateLimiterFactory);
}
