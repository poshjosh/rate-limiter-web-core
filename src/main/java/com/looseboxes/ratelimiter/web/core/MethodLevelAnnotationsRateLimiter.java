package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateExceededHandler;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.annotation.AnnotatedElementIdProvider;
import com.looseboxes.ratelimiter.annotation.MethodLevelAnnotationRateFactory;

import java.lang.reflect.Method;
import java.util.List;

public class MethodLevelAnnotationsRateLimiter<R> extends PathPatternsRateLimiter<R> {
    public MethodLevelAnnotationsRateLimiter(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<Class<?>> classes,
            AnnotatedElementIdProvider<Method, PathPatterns<R>> annotatedElementIdProvider) {
        super(rateSupplier, rateExceededHandler,
                new MethodLevelAnnotationRateFactory<>(classes, annotatedElementIdProvider).getRates());
    }
}
