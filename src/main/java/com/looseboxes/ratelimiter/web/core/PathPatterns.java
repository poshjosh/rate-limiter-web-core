package com.looseboxes.ratelimiter.web.core;

import java.util.Collections;
import java.util.List;

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
