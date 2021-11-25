package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.*;

import java.util.List;

public class ClassPatternsRateLimiter<R> extends PathPatternsRateLimiter<Class<?>, R, String>{

    public ClassPatternsRateLimiter(
            List<Class<?>> classes,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            IdProvider<Class<?>, PathPatterns<String>> idProvider) {
        super(classes, new ClassAnnotationProcessor(), new ClassAnnotationCollector(),
                rateLimiterConfigurationSource, idProvider);
    }
}
