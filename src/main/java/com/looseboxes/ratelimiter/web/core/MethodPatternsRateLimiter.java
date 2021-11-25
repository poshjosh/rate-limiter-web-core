package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MethodPatternsRateLimiter<R> extends PathPatternsRateLimiter<Method, R, String>{

    public MethodPatternsRateLimiter(
            List<Class<?>> classes,
            RateLimiterConfigurationRegistry<R> rateLimiterConfigurationRegistry,
            IdProvider<Method, PathPatterns<String>> idProvider) {
        super(
                classes.stream().flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods())).collect(Collectors.toList()),
                new MethodAnnotationProcessor(),
                new MethodAnnotationCollector(),
                rateLimiterConfigurationRegistry,
                idProvider);
    }
}
