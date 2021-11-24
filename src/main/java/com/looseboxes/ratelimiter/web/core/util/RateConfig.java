package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.rates.LimitWithinDuration;
import com.looseboxes.ratelimiter.rates.Rate;

import java.util.concurrent.TimeUnit;

public class RateConfig {

    private int limit;
    private long duration;
    private TimeUnit timeUnit;

    public Rate toRate() {
        return new LimitWithinDuration(limit, timeUnit.toMillis(duration));
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public String toString() {
        return "RateConfig{" +
                "duration=" + duration +
                ", limit=" + limit +
                ", timeUnit=" + timeUnit +
                '}';
    }
}
