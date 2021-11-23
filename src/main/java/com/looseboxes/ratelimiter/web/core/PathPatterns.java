package com.looseboxes.ratelimiter.web.core;

import java.util.Collections;
import java.util.List;

/**
 * Represents zero or more path patterns.
 *
 * Should support both path variables of the form {@code /users/{id}} and ant style path patterns as depicted in
 * the following table:
 *
 * +----------+-----------------------------------+
 * | Wildcard |            Description            |
 * +----------+-----------------------------------+
 * | ?        | Matches exactly one character.    |
 * | *        | Matches zero or more characters.  |
 * | **       | Matches zero or more directories. |
 * +----------+-----------------------------------+
 * @param <REQUEST>
 */
public interface PathPatterns<REQUEST> {

    PathPatterns<Object> NONE = new PathPatterns<Object>() {
        @Override
        public PathPatterns<Object> combine(PathPatterns<Object> other) {
            return other;
        }
        @Override
        public boolean matches(Object request) {
            return false;
        }
        @Override
        public List<String> getPathPatterns() { return Collections.emptyList(); }

    };

    @SuppressWarnings("unchecked")
    static <T> PathPatterns<T> none() {
        return (PathPatterns<T>)NONE;
    }

    PathPatterns<REQUEST> combine(PathPatterns<REQUEST> other);

    boolean matches(REQUEST request);

    List<String> getPathPatterns();
}
