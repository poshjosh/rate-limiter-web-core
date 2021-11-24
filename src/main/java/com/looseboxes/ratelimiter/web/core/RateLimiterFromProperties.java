package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.rates.Rates;
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

            final Map<String, List<Rate>> limitMap = toRateLists(properties);

            final int size = limitMap.size();

            this.requestToIdConverters = new ArrayList<>(size);
            this.rateLimiters = new ArrayList<>(size);

            Set<Map.Entry<String, List<Rate>>> entrySet = limitMap.entrySet();
            for(Map.Entry<String, List<Rate>> entry : entrySet) {
                String name = entry.getKey();
                RequestToIdConverter<R> requestToIdConverter = requestToIdConverterRegistry.getConverterOrDefault(name);
                List<Rate> limits = entry.getValue();
                Rates.Logic logic = properties.getRateLimitConfigs().get(name).getLogic();
                RateLimiter<Object> rateLimiter = new DefaultRateLimiter<>(rateCache, rateSupplier, logic, rateExceededHandler, limits.toArray(new Rate[0]));
                LOG.debug("Request to id converter: {}, RateLimiter: {}", requestToIdConverter, rateLimiter);

                this.requestToIdConverters.add(requestToIdConverter);
                this.rateLimiters.add(rateLimiter);
            }
        }
    }

    private Map<String, List<Rate>> toRateLists(RateLimitProperties properties) {
        final Map<String, List<Rate>> rateMap;
        final Map<String, RateConfigList> rateLimitConfigs = properties.getRateLimitConfigs();
        if(isDisabled(properties)) {
            rateMap = Collections.emptyMap();
        }else if(rateLimitConfigs == null || rateLimitConfigs.isEmpty()) {
            rateMap = Collections.emptyMap();
        }else {
            Map<String, List<Rate>> temp = new LinkedHashMap<>(rateLimitConfigs.size());
            rateLimitConfigs.forEach((name, rateLimitConfigList) -> {
                List<Rate> rateList = rateLimitConfigList.toRateList();
                temp.put(name, rateList);
            });
            rateMap = Collections.unmodifiableMap(temp);
        }
        return rateMap;
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
