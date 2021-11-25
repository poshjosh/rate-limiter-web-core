package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import com.looseboxes.ratelimiter.rates.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RateLimiterImpl<R> implements RateLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterImpl.class);

    private final List<RequestToIdConverter<R>> requestToIdConverters;

    private final List<RateLimiter<Object>> rateLimiters;

    public RateLimiterImpl(
            Map<String, RateLimitConfig> rateLimitConfigs,
            RateLimiterConfigurationRegistry<R> rateLimiterConfigurationRegistry) {
        final int size = rateLimitConfigs.size();
        this.requestToIdConverters = new ArrayList<>(size);
        this.rateLimiters = new ArrayList<>(size);

        Set<Map.Entry<String, RateLimitConfig>> entrySet = rateLimitConfigs.entrySet();
        for(Map.Entry<String, RateLimitConfig> entry : entrySet) {
            String groupName = entry.getKey();
            RequestToIdConverter<R> converter = rateLimiterConfigurationRegistry.getRequestToIdConverterOrDefault(groupName);
            this.requestToIdConverters.add(converter);

            final RateLimiterConfiguration<Object> config = rateLimiterConfigurationRegistry
                    .copyConfigurationOrDefault(groupName).rateLimitConfig(entry.getValue());
            RateLimiter<Object> rateLimiter = new DefaultRateLimiter<>(config);
            this.rateLimiters.add(rateLimiter);
            LOG.debug("name: {}, Request to id converter: {}, config: {}", groupName, converter, config);
        }
    }

    public Rate record(R request) throws RateLimitExceededException {

        Rate result = null;
        RateLimitExceededException exception = null;

        final int size = rateLimiters.size();
        for(int i=0; i<size; i++) {
            final RequestToIdConverter<R> requestToIdConverter = requestToIdConverters.get(i);
            final Object id = requestToIdConverter.convert(request);
            if(id == null) {
                continue;
            }
            RateLimiter<Object> rateLimiter = rateLimiters.get(i);
            try {
                Rate rate = rateLimiter.record(id);
                if(result == null) {
                    result = rate;
                }
            }catch(RateLimitExceededException e) {
                if(exception == null) {
                    exception = e;
                }else{
                    exception.addSuppressed(e);
                }
            }
        }

        if(exception != null) {
            throw exception;
        }

        return result;
    }
}
