package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rate;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.util.List;
import java.util.Objects;

public final class RequestRates extends Rates {

    public static RequestRates ofDefaults() {
        return new RequestRates();
    }

    public static RequestRates of(MatchConfig matchConfig) {
        return new RequestRates(matchConfig);
    }

    public static RequestRates of(Rates rates, MatchConfig matchConfig) {
        return new RequestRates(rates, matchConfig);
    }

    public static RequestRates of(Operator operator, List<Rate> limits, MatchConfig matchConfig) {
        return new RequestRates(operator, limits, matchConfig);
    }

    private MatchConfig matchConfig = new MatchConfig();

    // A public no-argument constructor is required
    public RequestRates() { }

    public RequestRates(MatchConfig matchConfig) {
        this.matchConfig = matchConfig;
    }

    public RequestRates(Rates rates, MatchConfig matchConfig) {
        super(rates);
        this.matchConfig = matchConfig;
    }

    public RequestRates(Operator operator, List<Rate> limits, MatchConfig matchConfig) {
        super(operator, limits);
        this.matchConfig = matchConfig;
    }

    public MatchConfig getMatchConfig() {
        return matchConfig;
    }

    public void setMatchConfig(MatchConfig matchConfig) {
        this.matchConfig = matchConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        RequestRates that = (RequestRates) o;
        return matchConfig.equals(that.matchConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), matchConfig);
    }

    @Override
    public String toString() {
        return "RequestRates{" + "matchConfig=" + matchConfig + '}';
    }
}
