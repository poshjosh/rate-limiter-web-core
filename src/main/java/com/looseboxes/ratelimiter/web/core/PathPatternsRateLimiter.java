package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.AnnotationCollector;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.cache.SingletonRateCache;
import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.util.RateLimitGroupData;
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
            AnnotationCollector<S, Map<String, RateLimitGroupData<S>>> annotationCollector,
            RateLimiterConfigurationSource<R> rateLimiterConfigurationSource,
            IdProvider<S, PathPatterns<K>> idProvider){

        this.requestPathPatterns = new ArrayList<>();
        this.rateLimiters = new ArrayList<>();

        sources.forEach(source -> annotationProcessor.process(source, annotationCollector));

        Map<String, RateLimitGroupData<S>> rateLimitConfigs = annotationCollector.getResult();
        Set<Map.Entry<String, RateLimitGroupData<S>>> entrySet = rateLimitConfigs.entrySet();
        for(Map.Entry<String, RateLimitGroupData<S>> entry : entrySet) {
            String groupName = entry.getKey();
            RateLimitGroupData<S> groupData = entry.getValue();
            Collection<S> groupMembers = groupData.getMembers();

            if (!groupMembers.isEmpty()) {

                for(S groupMember : groupMembers) {

                    PathPatterns<K> pathPatterns = idProvider.getId(groupMember);

                    this.requestPathPatterns.add(pathPatterns);

                    RateLimiterConfiguration<Object> rateLimiterConfiguration =
                            rateLimiterConfigurationSource.copyConfigurationOrDefault(groupName)
                                    .rateCache(new SingletonRateCache<>(pathPatterns))
                                    .rateLimitConfig(groupData.getConfig());
                    RateLimiter<Object> rateLimiter = new DefaultRateLimiter<>(rateLimiterConfiguration);
                    this.rateLimiters.add(rateLimiter);
                }
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
