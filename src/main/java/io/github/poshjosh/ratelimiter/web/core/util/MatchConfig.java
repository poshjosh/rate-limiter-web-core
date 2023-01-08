package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.Arrays;
import java.util.Objects;

public final class MatchConfig {

    private MatchType matchType = MatchType.NOOP;

    private String name = "";

    private String[] values = new String[0];

    private Operator operator = Operator.AND;

    public MatchConfig() {
        // A public no-argument constructor is required by this contract
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MatchConfig that = (MatchConfig) o;
        return matchType == that.matchType && name.equals(that.name) && Arrays
                .equals(values, that.values) && operator == that.operator;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(matchType, name, operator);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString() {
        return "MatchConfig{" + "matchType=" + matchType + ", " + name + "="
                + Arrays.toString(values) + ", operator=" + operator + '}';
    }
}
