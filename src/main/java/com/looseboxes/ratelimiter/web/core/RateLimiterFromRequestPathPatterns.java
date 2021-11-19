package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.RateComposition;
import com.looseboxes.ratelimiter.rates.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RateLimiterFromRequestPathPatterns<R> implements RateLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterFromRequestPathPatterns.class);

    private final RequestPathPatterns<R>[] requestPathPatternArray;
    private final RateLimiter<RequestPathPatterns<R>> [] rateLimiterArray;

    public RateLimiterFromRequestPathPatterns(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<RateComposition<RequestPathPatterns<R>>> limits) {

        LOG.debug("Limits: {}", limits);

        final int size = limits.size();

        this.requestPathPatternArray = new RequestPathPatterns[size];
        this.rateLimiterArray = new RateLimiter[size];

        if(!limits.isEmpty()) {

            for(int i = 0; i < size; i++) {

                RateComposition<RequestPathPatterns<R>> limit = limits.get(i);
                RequestPathPatterns<R> requestPathPatterns = limit.getId();
                this.requestPathPatternArray[i] = requestPathPatterns;
                this.rateLimiterArray[i] = new RateLimiterSingleton<>(
                        requestPathPatterns, rateSupplier, limit.getLogic(), rateExceededHandler, limit.getRates()
                );
            }
        }
    }

    @Override
    public Rate record(R request) throws RateLimitExceededException {

        LOG.trace("Invoking {} rate limiters for {}", rateLimiterArray.length, request);

        for(int i = 0; i< rateLimiterArray.length; i++) {

            RequestPathPatterns<R> requestPathPatterns = requestPathPatternArray[i];

            if(requestPathPatterns.matches(request)) {

                final RateLimiter<RequestPathPatterns<R>> rateLimiter = rateLimiterArray[i];

                return rateLimiter.record(requestPathPatterns);
            }
        }

        return Rate.NONE;
    }
}
