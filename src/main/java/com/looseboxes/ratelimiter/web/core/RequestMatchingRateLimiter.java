package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.RateLimiter;
import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.util.*;

public final class RequestMatchingRateLimiter<R> implements RateLimiter<R>{

    private final Matcher<R> matcher;
    private final RateLimiter<Object> rateLimiter;

    public RequestMatchingRateLimiter(Matcher<R> matcher, RateLimiter<Object> rateLimiter) {
        this.matcher = Objects.requireNonNull(matcher);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    public Rate record(R request) {
        if(matcher.matches(request)) {
            return rateLimiter.record(matcher.getId(request));
        }else{
            return Rate.NONE;
        }
    }

    public Matcher<R> getMatcher() {
        return matcher;
    }

    public RateLimiter<Object> getRateLimiter() {
        return rateLimiter;
    }

    @Override
    public String toString() {
        return "RequestMatchingRateLimiter{" +
                "matcher=" + matcher +
                ", rateLimiter=" + rateLimiter +
                '}';
    }
}
