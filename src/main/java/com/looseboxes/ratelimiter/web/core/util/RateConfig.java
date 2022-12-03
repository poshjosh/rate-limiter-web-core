package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.rates.Rate;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

public class RateConfig implements Serializable {

    private static final long serialVersionUID = 9081726354000000005L;

    public static RateConfig of(long limit, Duration duration) {
        return new RateConfig(limit, duration);
    }

    private long limit;
    private Duration duration;

    public RateConfig() { }

    public RateConfig(RateConfig rateConfig) {
        this(rateConfig.limit, rateConfig.duration);
    }

    private RateConfig(long limit, Duration duration) {
        this.limit = limit;
        this.duration = duration;
    }

    public Rate toRate() {
        return Rate.of(limit, duration.toMillis());
    }

    public RateConfig limit(long limit) {
        this.setLimit(limit);
        return this;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public RateConfig duration(Duration duration) {
        this.setDuration(duration);
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RateConfig that = (RateConfig) o;
        return limit == that.limit && Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, duration);
    }

    @Override
    public String toString() {
        return "RateConfig{limit=" + limit + ", duration=" + duration + '}';
    }
}
