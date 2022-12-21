package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.util.Operator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RateLimitConfig{

    private Operator operator = Operator.OR;

    private List<RateConfig> limits;

    public RateLimitConfig() { }

    public RateLimitConfig(RateLimitConfig rateLimitConfig) {
        this.operator = rateLimitConfig.operator;
        this.limits = rateLimitConfig.limits == null ? null : rateLimitConfig.limits.stream()
                .map(RateConfig::new).collect(Collectors.toList());
    }

    public RateLimitConfig logic(Operator operator) {
        setLogic(operator);
        return this;
    }

    public Operator getLogic() {
        return operator;
    }

    public void setLogic(Operator operator) {
        this.operator = operator;
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
        return operator == that.operator && Objects.equals(limits, that.limits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, limits);
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" + "operator=" + operator + ", limits=" + limits + '}';
    }
}
