package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.BandwidthFactory;

import java.time.Duration;
import java.util.Objects;

public class RateConfig{

    public static RateConfig of(long limit, Duration duration) {
        return of(limit, duration, BandwidthFactory.SmoothBurstyBandwidthFactory.class);
    }

    public static RateConfig of(long limit, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        return new RateConfig(limit, duration, factoryClass);
    }

    private long limit;
    private Duration duration;
    private Class<? extends BandwidthFactory> factoryClass;

    public RateConfig() { }

    public RateConfig(RateConfig rateConfig) {
        this(rateConfig.limit, rateConfig.duration, rateConfig.factoryClass);
    }

    private RateConfig(long limit, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        this.limit = limit;
        this.duration = Objects.requireNonNull(duration);
        this.factoryClass = Objects.requireNonNull(factoryClass);
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

    public RateConfig factoryClass(Class<? extends BandwidthFactory> factoryClass) {
        setFactoryClass(factoryClass);
        return this;
    }

    public Class<? extends BandwidthFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends BandwidthFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateConfig that = (RateConfig) o;
        return limit == that.limit && duration.equals(that.duration) && factoryClass.equals(that.factoryClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, duration, factoryClass);
    }

    @Override
    public String toString() {
        return "RateConfig{" +
                "limit=" + limit +
                ", duration=" + duration +
                ", factoryClass=" + factoryClass +
                '}';
    }
}
