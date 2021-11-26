package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateFactory;
import com.looseboxes.ratelimiter.cache.RateCache;

import java.lang.reflect.Method;

public interface RateLimiterConfigurationRegistry<R> {

    void registerRequestToIdConverter(Class<?> clazz, RequestToIdConverter<R> requestToIdConverter);

    void registerRequestToIdConverter(Method method, RequestToIdConverter<R> requestToIdConverter);

    void registerRequestToIdConverter(String name, RequestToIdConverter<R> requestToIdConverter);

    void registerRateCache(Class<?> clazz, RateCache rateCache);

    void registerRateCache(Method method, RateCache rateCache);

    void registerRateCache(String name, RateCache rateCache);

    void registerRateFactory(Class<?> clazz, RateFactory rateFactory);

    void registerRateFactory(Method method, RateFactory rateFactory);

    void registerRateFactory(String name, RateFactory rateFactory);

    void registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener);

    void registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener);

    void registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener);
}
