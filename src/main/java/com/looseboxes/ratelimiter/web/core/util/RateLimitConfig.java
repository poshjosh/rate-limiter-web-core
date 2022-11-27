package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.Limit;
import com.looseboxes.ratelimiter.rates.Logic;
import com.looseboxes.ratelimiter.rates.Rate;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RateLimitConfig implements Serializable {

    private static final long serialVersionUID = 9081726354000000006L;

    private Logic logic = Logic.OR;

    private List<RateConfig> limits;

    public RateLimitConfig() { }

    public RateLimitConfig(RateLimitConfig rateLimitConfig) {
        this.logic = rateLimitConfig.logic;
        this.limits = rateLimitConfig.limits == null ? null : rateLimitConfig.limits.stream()
                .map(RateConfig::new).collect(Collectors.toList());
    }

    public Limit toLimit() {
        if(limits == null || limits.isEmpty()) {
            return Limit.empty(logic);
        }else if(limits.size() == 1) {
            return Limit.of(logic, limits.get(0).toRate());
        }else {
            return Limit.of(logic, limits.stream().map(RateConfig::toRate).toArray(Rate[]::new));
        }
    }

    public RateLimitConfig logic(Logic logic) {
        setLogic(logic);
        return this;
    }

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public RateLimitConfig limits(List<RateConfig> limits) {
        setLimits(limits);
        return this;
    }

    public List<RateConfig> getLimits() {
        return limits;
    }

    public void setLimits(List<RateConfig> limits) {
        this.limits = limits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RateLimitConfig that = (RateLimitConfig) o;
        return logic == that.logic && Objects.equals(limits, that.limits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logic, limits);
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" + "logic=" + logic + ", limits=" + limits + '}';
    }
}
