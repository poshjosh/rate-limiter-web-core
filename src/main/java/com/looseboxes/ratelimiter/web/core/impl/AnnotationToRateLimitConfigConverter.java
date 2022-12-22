package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.RateLimit;
import com.looseboxes.ratelimiter.annotation.RateLimitGroup;
import com.looseboxes.ratelimiter.util.Operator;
import com.looseboxes.ratelimiter.web.core.util.RateConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class AnnotationToRateLimitConfigConverter implements AnnotationProcessor.Converter<RateLimitConfig> {

    AnnotationToRateLimitConfigConverter() {}

    public boolean isOperatorEqual(RateLimitConfig rateLimitConfig, Operator operator) {
        return rateLimitConfig.getOperator().equals(operator);
    }

    @Override
    public RateLimitConfig convert(RateLimitGroup rateLimitGroup, RateLimit[] rateLimits) {
        final Operator operator = operator(rateLimitGroup);
        if (rateLimits.length == 0) {
            return new RateLimitConfig().limits(Collections.emptyList());
        }
        final RateConfig[] configs = new RateConfig[rateLimits.length];
        for (int i = 0; i < rateLimits.length; i++) {
            configs[i] = createRateConfig(rateLimits[i]);
        }
        return new RateLimitConfig().operator(operator).limits(Arrays.asList(configs));
    }

    private Operator operator(RateLimitGroup rateLimitGroup) {
        return rateLimitGroup == null ? AnnotationProcessor.DEFAULT_OPERATOR : rateLimitGroup.logic();
    }

    private RateConfig createRateConfig(RateLimit rateLimit) {
        Duration duration = Duration.of(rateLimit.duration(), chronoUnit(rateLimit.timeUnit()));
        return RateConfig.of(rateLimit.limit(), duration, rateLimit.factoryClass());
    }

    private ChronoUnit chronoUnit(TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit);
        if(TimeUnit.NANOSECONDS.equals(timeUnit)) {
            return ChronoUnit.NANOS;
        }
        if(TimeUnit.MICROSECONDS.equals(timeUnit)) {
            return ChronoUnit.MICROS;
        }
        if(TimeUnit.MILLISECONDS.equals(timeUnit)) {
            return ChronoUnit.MILLIS;
        }
        if(TimeUnit.SECONDS.equals(timeUnit)) {
            return ChronoUnit.SECONDS;
        }
        if(TimeUnit.MINUTES.equals(timeUnit)) {
            return ChronoUnit.MINUTES;
        }
        if(TimeUnit.HOURS.equals(timeUnit)) {
            return ChronoUnit.HOURS;
        }
        if(TimeUnit.DAYS.equals(timeUnit)) {
            return ChronoUnit.DAYS;
        }
        throw new Error("Unexpected TimeUnit: " + timeUnit);
    }
}
