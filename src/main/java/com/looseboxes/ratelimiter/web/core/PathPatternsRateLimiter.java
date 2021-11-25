package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.AnnotationCollector;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.RateLimitGroupMembers;
import com.looseboxes.ratelimiter.cache.SingletonRateCache;
import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PathPatternsRateLimiter<S, R, K> implements RateLimiter<K> {

    private static final Logger LOG = LoggerFactory.getLogger(PathPatternsRateLimiter.class);

    private final List<PathPatterns<K>> requestPathPatterns;
    private final List<RateLimiter<Object>> rateLimiters;

    public PathPatternsRateLimiter(
            List<S> sources,
            AnnotationProcessor<S> annotationProcessor,
            AnnotationCollector<S, Map<RateLimitGroupMembers<S>, RateLimitConfig>> annotationCollector,
            RateLimiterConfigurationRegistry<R> rateLimiterConfigurationRegistry,
            IdProvider<S, PathPatterns<K>> idProvider){

        this.requestPathPatterns = new ArrayList<>();
        this.rateLimiters = new ArrayList<>();

        sources.forEach(source -> annotationProcessor.process(source, annotationCollector));
        Map<RateLimitGroupMembers<S>, RateLimitConfig> rateLimitConfigs = annotationCollector.getResult();
        Set<Map.Entry<RateLimitGroupMembers<S>, RateLimitConfig>> entrySet = rateLimitConfigs.entrySet();
        for(Map.Entry<RateLimitGroupMembers<S>, RateLimitConfig> entry : entrySet) {
            RateLimitGroupMembers<S> group = entry.getKey();
            String name = group.getName();
            Collection<S> members = group.getMembers();

            if (!members.isEmpty()) {

                PathPatterns<K> pathPatterns = idProvider.getId(members.iterator().next());
                this.requestPathPatterns.add(pathPatterns);

                RateLimiterConfiguration<Object> rateLimiterConfiguration =
                        rateLimiterConfigurationRegistry.copyConfigurationOrDefault(name)
                                .rateCache(new SingletonRateCache<>(pathPatterns))
                                .rateLimitConfig(entry.getValue());
                RateLimiter<Object> rateLimiter = new DefaultRateLimiter<>(rateLimiterConfiguration);
                this.rateLimiters.add(rateLimiter);
            }
        }
    }

    @Override
    public Rate record(K request) throws RateLimitExceededException {

        LOG.trace("Invoking {} rate limiters for {}", rateLimiters.size(), request);

        Rate result = Rate.NONE;
        RateLimitExceededException exception = null;

        final int size = rateLimiters.size();

        for(int i = 0; i < size; i++) {

            PathPatterns<K> pathPatterns = requestPathPatterns.get(i);

            if(pathPatterns.matches(request)) {

                final RateLimiter<Object> rateLimiter = rateLimiters.get(i);

                try {
                    result = rateLimiter.record(pathPatterns);
                }catch(RateLimitExceededException e) {
                    if(exception == null) {
                        exception = e;
                    }else{
                        exception.addSuppressed(e);
                    }
                }
            }
        }

        if(exception != null) {
            throw exception;
        }

        return result;
    }
}
