package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.rates.Rates;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RateLimitConfig {

    private Rates.Logic logic = Rates.Logic.OR;

    private List<RateConfig> limits;

    public RateLimitConfig() { }

    public List<Rate> toRateList() {
        if(limits == null || limits.isEmpty()) {
            return Collections.emptyList();
        }else if(limits.size() == 1) {
            return Collections.singletonList(limits.get(0).toRate());
        }else {
            return limits.stream().map(RateConfig::toRate).collect(Collectors.toList());
        }
    }

    public Rates.Logic getLogic() {
        return logic;
    }

    public void setLogic(Rates.Logic logic) {
        this.logic = logic;
    }

    public List<RateConfig> getLimits() {
        return limits;
    }

    public void setLimits(List<RateConfig> limits) {
        this.limits = limits;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "logic=" + logic +
                ", limits=" + limits +
                '}';
    }
}
