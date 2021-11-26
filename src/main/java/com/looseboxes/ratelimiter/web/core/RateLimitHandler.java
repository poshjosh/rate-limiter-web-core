package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimitExceededException;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationCollector;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.RateLimitGroupData;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RateLimitHandler<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitHandler.class);

    private final RateLimitProperties properties;
    private final RateLimiter<R> rateLimiter;
    private final RequestToIdConverter<R> requestToIdConverter;
    private final RateLimiter<String> classRateLimiter;
    private final RateLimiter<String> methodRateLimiter;

    public RateLimitHandler(
            RateLimitProperties properties,
            RateLimiter<R> rateLimiter,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            ResourceClassesSupplier resourceClassesSupplier,
            AnnotationProcessor<Class<?>> classAnnotationProcessor,
            AnnotationProcessor<Method> methodAnnotationProcessor,
            AnnotationCollector<Class<?>, Map<String, RateLimitGroupData<Class<?>>>> classAnnotationCollector,
            AnnotationCollector<Method, Map<String, RateLimitGroupData<Method>>> methodAnnotationCollector,
            IdProvider<Class<?>, PathPatterns<String>> classIdProvider,
            IdProvider<Method, PathPatterns<String>> methodIdProvider) {

        this.properties = Objects.requireNonNull(properties);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.requestToIdConverter = rateLimiterConfigurationSource.getDefaultRequestToIdConverter();

        List<Class<?>> classes = resourceClassesSupplier.get();
        this.classRateLimiter = classes.isEmpty() ? RateLimiter.noop() : new PathPatternsRateLimiter<>(
                classes, classAnnotationProcessor, classAnnotationCollector, rateLimiterConfigurationSource, classIdProvider);

        List<Method> methods = classes.stream()
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .collect(Collectors.toList());
        this.methodRateLimiter = classes.isEmpty() ? RateLimiter.noop() : new PathPatternsRateLimiter<>(
                methods, methodAnnotationProcessor, methodAnnotationCollector, rateLimiterConfigurationSource, methodIdProvider);
    }

    public void handleRequest(R request) {

        RateLimitExceededException exception = null;

        if(isAuto()) {
            exception = record(rateLimiter, request, exception);
        }

        final String requestUri = requestToIdConverter.convert(request).toString();

        LOG.trace("Request URI: {}, request: {}", requestUri, request);

        exception = record(classRateLimiter, requestUri, exception);

        exception = record(methodRateLimiter, requestUri, exception);

        if(exception != null) {
            throw exception;
        }
    }
    
    private <K> RateLimitExceededException record(RateLimiter<K> rateLimiter, K key, RateLimitExceededException exception) {
        if(isDisabled()) {
            return exception;
        }
        try {
            if(LOG.isTraceEnabled()) {
                LOG.trace("Recording key: {}, with: {}", key, rateLimiter);
            }
            rateLimiter.record(key);
        }catch(RateLimitExceededException e) {
            if(exception == null) {
                return e;
            }
            exception.addSuppressed(e);
        }
        return exception;
    }

    private boolean isAuto() {
        return Boolean.TRUE.equals(properties.getAuto());
    }

    private boolean isDisabled() {
        return Boolean.TRUE.equals(properties.getDisabled());
    }
}
