package io.github.poshjosh.ratelimiter.web.core.util;

import java.io.Serializable;
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
public interface PathPatterns<PATH> extends Serializable {

    PathPatterns<Object> NONE = new PathPatterns<Object>() {
        @Override
        public PathPatterns<Object> combine(PathPatterns<Object> other) {
            return other;
        }
        @Override
        public List<String> getPatterns() { return Collections.emptyList(); }
        @Override
        public boolean matches(Object path) {
            return false;
        }
        @Override public String toString() {
            return "PathPatterns$NONE";
        }
    };

    @SuppressWarnings("unchecked")
    static <T> PathPatterns<T> none() {
        return (PathPatterns<T>)NONE;
    }

    PathPatterns<PATH> combine(PathPatterns<PATH> other);

    List<String> getPatterns();

    boolean matches(PATH path);
}
