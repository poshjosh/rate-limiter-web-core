package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.model.Operator;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.util.*;

final class MatchUtils {
    private MatchUtils() {}
    static <T> boolean matchesList(Operator operator, List<T> values, T[] supplied) {
        switch(operator) {
            case AND: return allMatch(values, supplied);
            case OR: return anyMatch(values, supplied);
            default: throw invalidOperator(operator);
        }
    }
    static <T> boolean matchesValue(Operator operator, T value, T[] supplied) {
        switch(operator) {
            case AND: return allMatch(value, supplied);
            case OR: return anyMatch(value, supplied);
            default: throw invalidOperator(operator);
        }
    }
    private static RuntimeException invalidOperator(Operator operator) {
        return new UnsupportedOperationException("Unexpected " +
                Operator.class.getName() + ": " + operator);
    }
    private static <T> boolean allMatch(List<T> values, T[] supplied) {
        if (isNullOrEmpty(values)) {
            return false;
        }
        if (supplied.length == 0) {
            return false;
        }
        // All supplied must be found in values
        for (T candidate : supplied) {
            if (!values.contains(candidate)) {
                return false;
            }
        }
        return true;
    }
    private static <T> boolean allMatch(T value, T[] supplied) {
        if (isNullOrEmpty(value)) {
            return false;
        }
        if (supplied.length == 0) {
            return false;
        }
        if (supplied.length == 1) {
            return Objects.equals(value, supplied[0]);
        }
        return Arrays.stream(supplied).allMatch(candidate -> Objects.equals(value, candidate));
    }
    private static <T> boolean anyMatch(List<T> values, T[] supplied) {
        if (isNullOrEmpty(values)) {
            return false;
        }
        if (supplied.length == 0) {
            return false;
        }
        // Any candidate must be found in values
        for (T candidate : supplied) {
            if (values.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
    private static <T> boolean anyMatch(T value, T[] supplied) {
        if (isNullOrEmpty(value)) {
            return false;
        }
        if (supplied.length == 0) {
            return false;
        }
        if (supplied.length == 1) {
            return Objects.equals(value, supplied[0]);
        }
        return Arrays.asList(supplied).contains(value);
    }
    private static boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
    private static boolean isNullOrEmpty(Object o) {
        return o == null || !StringUtils.hasText(o.toString());
    }
}
