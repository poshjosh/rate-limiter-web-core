package com.looseboxes.ratelimiter.web.core;

import java.util.Collections;
import java.util.List;

/**
 * Represents zero or more path patterns.
 *
 * Supports both path variables of the form {@code /users/{id}} and ant style path patterns as depicted in
 * the following table:
 *
 * +----------+-----------------------------------+
 * | Wildcard |            Description            |
 * +----------+-----------------------------------+
 * | ?        | Matches exactly one character.    |
 * | *        | Matches zero or more characters.  |
 * | **       | Matches zero or more directories. |
 * +----------+-----------------------------------+
 * @param <PATH> The of object that may be matched
 */
public interface PathPatterns<PATH> {

    PathPatterns<Object> NONE = new PathPatterns<Object>() {
        @Override
        public PathPatterns<Object> combine(PathPatterns<Object> other) {
            return other;
        }
        @Override
        public boolean matches(Object path) {
            return false;
        }
        @Override
        public List<String> getPathPatterns() { return Collections.emptyList(); }

    };

    @SuppressWarnings("unchecked")
    static <T> PathPatterns<T> none() {
        return (PathPatterns<T>)NONE;
    }

    PathPatterns<PATH> combine(PathPatterns<PATH> other);

    boolean matches(PATH path);

    List<String> getPathPatterns();
}
