package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateExceededHandler;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.annotation.AnnotatedElementIdProvider;
import com.looseboxes.ratelimiter.annotation.ClassLevelAnnotationRateFactory;

import java.util.List;

public class ClassLevelAnnotationRateLimiter<R> extends PathPatternsRateLimiter<R> {
    public ClassLevelAnnotationRateLimiter(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<Class<?>> classes,
            AnnotatedElementIdProvider<Class<?>, PathPatterns<R>> annotatedElementIdProvider) {
        super(rateSupplier, rateExceededHandler,
                new ClassLevelAnnotationRateFactory<>(classes, annotatedElementIdProvider).getRates());
    }
}
