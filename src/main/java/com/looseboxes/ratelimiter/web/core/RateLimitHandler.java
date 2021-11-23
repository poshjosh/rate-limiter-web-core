package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateExceededHandler;
import com.looseboxes.ratelimiter.RateLimitExceededException;
import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.RateSupplier;
import com.looseboxes.ratelimiter.annotation.AnnotatedElementIdProvider;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RateLimitHandler<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitHandler.class);

    private final RateLimitProperties properties;
    private final RateLimiter<R> rateLimiter;
    private final RequestToIdConverter<R> requestToIdConverter;
    private final RateLimiter<String>[] rateLimiters;

    public RateLimitHandler(
            RateLimitProperties properties,
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            RateLimiter<R> rateLimiter,
            RequestToIdConverter<R> requestToIdConverter,
            AnnotatedElementIdProvider<Class<?>, PathPatterns<String>> annotatedElementIdProviderForClass,
            AnnotatedElementIdProvider<Method, PathPatterns<String>> annotatedElementIdProviderForMethod,
            List<Class<?>> classes) {
        this(
                properties,
                rateLimiter,
                requestToIdConverter,
                classes.isEmpty() ? RateLimiter.noop() :
                        new ClassLevelAnnotationRateLimiter<>(
                                rateSupplier, rateExceededHandler, classes,
                                annotatedElementIdProviderForClass
                        ),
                classes.isEmpty() ? RateLimiter.noop() :
                        new MethodLevelAnnotationsRateLimiter<>(
                                rateSupplier, rateExceededHandler, classes,
                                annotatedElementIdProviderForMethod
                        )
        );
    }

    @SafeVarargs
    public RateLimitHandler(
            RateLimitProperties properties,
            RateLimiter<R> rateLimiter,
            RequestToIdConverter<R> requestToIdConverter,
            RateLimiter<String>... rateLimiters) {
        this.properties = Objects.requireNonNull(properties);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.requestToIdConverter = Objects.requireNonNull(requestToIdConverter);
        this.rateLimiters = Objects.requireNonNull(rateLimiters);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Rate limiters\nFrom property: {}\nFrom annotations: {}",
                    rateLimiter, Arrays.toString(rateLimiters));
        }
    }

    public void handleRequest(R request) {

        RateLimitExceededException exception = null;

        if(isAuto()) {
            exception = record(rateLimiter, request, exception);
        }

        final String requestUri = requestToIdConverter.convert(request).toString();

        LOG.trace("Request URI: {}, request: {}", requestUri, request);

        for(RateLimiter<String> rateLimiter : rateLimiters) {
            exception = record(rateLimiter, requestUri, exception);
        }

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
