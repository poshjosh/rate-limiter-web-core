package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateRecordedListener;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.cache.RateCache;

import java.lang.reflect.Method;

public interface RateLimiterConfigurationRegistry<R> {

    void registerRequestToIdConverter(Class<?> clazz, RequestToIdConverter<R> requestToIdConverter);

    void registerRequestToIdConverter(Method method, RequestToIdConverter<R> requestToIdConverter);

    void registerRequestToIdConverter(String name, RequestToIdConverter<R> requestToIdConverter);

    void registerRateCache(Class<?> clazz, RateCache rateCache);

    void registerRateCache(Method method, RateCache rateCache);

    void registerRateCache(String name, RateCache rateCache);

    void registerRateSupplier(Class<?> clazz, RateSupplier rateSupplier);

    void registerRateSupplier(Method method, RateSupplier rateSupplier);

    void registerRateSupplier(String name, RateSupplier rateSupplier);

    void registerRateExceededHandler(Class<?> clazz, RateRecordedListener rateRecordedListener);

    void registerRateExceededHandler(Method method, RateRecordedListener rateRecordedListener);

    void registerRateExceededHandler(String name, RateRecordedListener rateRecordedListener);
}
