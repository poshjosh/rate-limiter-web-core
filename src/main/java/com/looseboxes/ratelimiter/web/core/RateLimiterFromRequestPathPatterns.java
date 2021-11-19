package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.RateComposition;
import com.looseboxes.ratelimiter.rates.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RateLimiterFromRequestPathPatterns<R> implements RateLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterFromRequestPathPatterns.class);

    private final PathPatterns<R>[] requestPathPatternArray;
    private final RateLimiter<PathPatterns<R>> [] rateLimiterArray;

    public RateLimiterFromRequestPathPatterns(
            RateSupplier rateSupplier,
            RateExceededHandler rateExceededHandler,
            List<RateComposition<PathPatterns<R>>> limits) {

        LOG.debug("Limits: {}", limits);

        final int size = limits.size();

        this.requestPathPatternArray = new PathPatterns[size];
        this.rateLimiterArray = new RateLimiter[size];

        if(!limits.isEmpty()) {

            for(int i = 0; i < size; i++) {

                RateComposition<PathPatterns<R>> limit = limits.get(i);
                PathPatterns<R> pathPatterns = limit.getId();
                this.requestPathPatternArray[i] = pathPatterns;
                this.rateLimiterArray[i] = new RateLimiterSingleton<>(
                        pathPatterns, rateSupplier, limit.getLogic(), rateExceededHandler, limit.getRates()
                );
            }
        }
    }

    @Override
    public Rate record(R request) throws RateLimitExceededException {

        LOG.trace("Invoking {} rate limiters for {}", rateLimiterArray.length, request);

        for(int i = 0; i< rateLimiterArray.length; i++) {

            PathPatterns<R> pathPatterns = requestPathPatternArray[i];

            if(pathPatterns.matches(request)) {

                final RateLimiter<PathPatterns<R>> rateLimiter = rateLimiterArray[i];

                return rateLimiter.record(pathPatterns);
            }
        }

        return Rate.NONE;
    }
}
