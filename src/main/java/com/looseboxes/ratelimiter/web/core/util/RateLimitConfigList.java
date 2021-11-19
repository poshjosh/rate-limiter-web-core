package com.looseboxes.ratelimiter.web.core.util;

import com.looseboxes.ratelimiter.rates.Rate;
import com.looseboxes.ratelimiter.rates.Rates;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RateLimitConfigList {

    private Rates.Logic logic = Rates.Logic.OR;

    private List<RateLimitConfig> limits;

    public RateLimitConfigList() { }

    public Rate toRate() {
        if(limits == null || limits.isEmpty()) {
            return Rate.NONE;
        }else if(limits.size() == 1) {
            return limits.get(0).toRate();
        }else {
            List<Rate> rateLimits = limits.stream().map(limit -> limit.toRate()).collect(Collectors.toList());
            return Rates.compose(logic, rateLimits.toArray(new Rate[0]));
        }
    }

    public List<Rate> toRateList() {
        if(limits == null || limits.isEmpty()) {
            return Collections.emptyList();
        }else if(limits.size() == 1) {
            return Collections.singletonList(limits.get(0).toRate());
        }else {
            return limits.stream().map(limit -> limit.toRate()).collect(Collectors.toList());
        }
    }

    public Rates.Logic getLogic() {
        return logic;
    }

    public void setLogic(Rates.Logic logic) {
        this.logic = logic;
    }

    public List<RateLimitConfig> getLimits() {
        return limits;
    }

    public void setLimits(List<RateLimitConfig> limits) {
        this.limits = limits;
    }

    @Override
    public String toString() {
        return "RateLimitConfigList{" +
                "logic=" + logic +
                ", limits=" + limits +
                '}';
    }
}
