package com.looseboxes.ratelimiter.web.core;

public interface PatternMatchingRateLimiterConfig<REQUEST> {

    PatternMatchingRateLimiterFactory<REQUEST> getPatternMatchingRateLimiterFactoryForProperties();

    PatternMatchingRateLimiterFactory<REQUEST> getPatternMatchingRateLimiterFactoryForAnnotations();
}
