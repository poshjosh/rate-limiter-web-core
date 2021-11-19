package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateExceededHandler;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.annotation.AnnotatedElementIdProvider;
import com.looseboxes.ratelimiter.annotation.RateFactoryForMethodLevelAnnotation;

import java.lang.reflect.Method;
import java.util.List;

public class RateLimiterFromMethodLevelAnnotations<R> extends RateLimiterFromRequestPathPatterns<R>{
    public RateLimiterFromMethodLevelAnnotations(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<Class<?>> classes,
            AnnotatedElementIdProvider<Method, PathPatterns<R>> annotatedElementIdProvider) {
        super(rateSupplier, rateExceededHandler,
                new RateFactoryForMethodLevelAnnotation<>(classes, annotatedElementIdProvider).getRates());
    }
}
