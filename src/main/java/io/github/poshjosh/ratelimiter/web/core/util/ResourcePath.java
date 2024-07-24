package io.github.poshjosh.ratelimiter.web.core.util;

import java.io.Serializable;
import java.util.List;

/**
 * Represents zero or more path patterns.
 * Supports both path variables of the form {@code /users/{id}} and ant style path patterns as depicted in
 * the following table:
 * +----------+-----------------------------------+
 * | Wildcard |            Description            |
 * +----------+-----------------------------------+
 * | ?        | Matches exactly one character.    |
 * | *        | Matches zero or more characters.  |
 * | **       | Matches zero or more directories. |
 * +----------+-----------------------------------+
 */
public interface ResourcePath extends Serializable {

    ResourcePath combine(ResourcePath other);

    List<String> getPatterns();

    boolean matches(String path);
}
