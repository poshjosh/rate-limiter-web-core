package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import com.looseboxes.ratelimiter.rates.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RateLimiterFromProperties<R> implements RateLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterFromProperties.class);

    private final List<RequestToIdConverter<R>> requestToIdConverters;

    private final List<RateLimiter<Object>> rateLimiters;

    public RateLimiterFromProperties(
            RateLimitProperties properties,
            RequestToIdConverterRegistry<R> requestToIdConverterRegistry,
            RateCache<Object> rateCache,
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler) {
        if(isDisabled(properties)) {
            this.requestToIdConverters = Collections.emptyList();
            this.rateLimiters = Collections.emptyList();
        }else {

            Map<String, RateLimitConfig> rateConfigGroupMap = properties.getRateLimitConfigs();
            final int size = rateConfigGroupMap.size();
            this.requestToIdConverters = new ArrayList<>(size);
            this.rateLimiters = new ArrayList<>(size);

            Set<Map.Entry<String, RateLimitConfig>> entrySet = rateConfigGroupMap.entrySet();
            for(Map.Entry<String, RateLimitConfig> entry : entrySet) {
                String groupName = entry.getKey();
                RequestToIdConverter<R> requestToIdConverter = requestToIdConverterRegistry.getConverterOrDefault(groupName);
                this.requestToIdConverters.add(requestToIdConverter);

                final RateLimitConfig rateLimitConfig = entry.getValue();
                RateLimiter<Object> rateLimiter = new DefaultRateLimiter<>(
                        rateCache, rateSupplier, rateLimitConfig.getLogic(), rateExceededHandler, rateLimitConfig.toRateList().toArray(new Rate[0]));
                this.rateLimiters.add(rateLimiter);
                LOG.debug("Request to id converter: {}, RateLimiter: {}", requestToIdConverter, rateLimiter);
            }
        }
    }

    private boolean isDisabled(RateLimitProperties properties) {
        return Boolean.TRUE.equals(properties.getDisabled());
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
