package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateExceededHandler;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.annotation.AnnotatedElementIdProvider;
import com.looseboxes.ratelimiter.annotation.RateFactoryForClassLevelAnnotation;

import java.util.List;

public class RateLimiterFromClassLevelAnnotations<R> extends RateLimiterFromRequestPathPatterns<R>{
    public RateLimiterFromClassLevelAnnotations(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<Class<?>> classes,
            AnnotatedElementIdProvider<Class<?>, PathPatterns<R>> annotatedElementIdProvider) {
        super(rateSupplier, rateExceededHandler,
                new RateFactoryForClassLevelAnnotation<>(classes, annotatedElementIdProvider).getRates());
    }
}
