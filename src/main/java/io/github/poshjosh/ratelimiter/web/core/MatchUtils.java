package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.*;

final class MatchUtils {
    private MatchUtils() {}
    static <T> String matchOrNull(Operator operator, String name, List<T> values, T[] candidates) {
        Objects.requireNonNull(operator);
        Objects.requireNonNull(name);
        if (candidates.length == 0 && !isNullOrEmpty(values)) { // Match name only
            return name;
        }
        if (matches(operator, values, candidates)) {
            return name + "=" + toString(candidates);
        }
        return null;
    }
    static <T> String matchOrNull(Operator operator, List<T> values, T[] candidates) {
        if (matches(operator, values, candidates)) {
            return toString(candidates);
        }
        return null;
    }
    static <T> String matchOrNull(Operator operator, String name, T value, T[] candidates) {
        Objects.requireNonNull(operator);
        Objects.requireNonNull(name);
        if (candidates.length == 0 && !isNullOrEmpty(value)) { // Match name only
            return name;
        }
        if (matches(operator, value, candidates)) {
            return name + "=" + toString(candidates);
        }
        return null;
    }
    static <T> String matchOrNull(Operator operator, T value, T[] candidates) {
        Objects.requireNonNull(operator);
        Objects.requireNonNull(candidates);
        if (matches(operator, value, candidates)) {
            return toString(candidates);
        }
        return null;
    }
    static String toString(Object[] candidates) {
        if (candidates.length == 0) {
            throw new IllegalArgumentException();
        }
        return candidates.length == 1 ? candidates[0].toString() : Arrays.toString(candidates);
    }
    private static <T> boolean matches(Operator operator, List<T> values, T[] candidates) {
        switch(operator) {
            case AND: return allMatch(values, candidates);
            case OR: return anyMatch(values, candidates);
            default: throw invalidOperator(operator);
        }
    }
    private static <T> boolean matches(Operator operator, T value, T[] candidates) {
        switch(operator) {
            case AND: return allMatch(value, candidates);
            case OR: return anyMatch(value, candidates);
            default: throw invalidOperator(operator);
        }
    }
    private static RuntimeException invalidOperator(Operator operator) {
        return new UnsupportedOperationException("Unexpected " +
                Operator.class.getName() + ": " + operator);
    }
    private static <T> boolean allMatch(List<T> values, T[] candidates) {
        if (isNullOrEmpty(values)) {
            return false;
        }
        if (candidates.length == 0) {
            return false;
        }
        // All candidates must be found in values
        for (T candidate : candidates) {
            if (!values.contains(candidate)) {
                return false;
            }
        }
        return true;
    }
    private static <T> boolean allMatch(T value, T[] candidates) {
        if (isNullOrEmpty(value)) {
            return false;
        }
        if (candidates.length == 0) {
            return false;
        }
        if (candidates.length == 1) {
            return Objects.equals(value, candidates[0]);
        }
        return Arrays.stream(candidates).allMatch(candidate -> Objects.equals(value, candidate));
    }
    private static <T> boolean anyMatch(List<T> values, T[] candidates) {
        if (isNullOrEmpty(values)) {
            return false;
        }
        if (candidates.length == 0) {
            return false;
        }
        // Any candidate must be found in values
        for (T candidate : candidates) {
            if (values.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
    private static <T> boolean anyMatch(T value, T[] candidates) {
        if (isNullOrEmpty(value)) {
            return false;
        }
        if (candidates.length == 0) {
            return false;
        }
        if (candidates.length == 1) {
            return Objects.equals(value, candidates[0]);
        }
        return Arrays.asList(candidates).contains(value);
    }
    private static boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
    private static boolean isNullOrEmpty(Object o) {
        return o == null || o.toString().isEmpty();
    }
}
