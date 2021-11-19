package com.looseboxes.ratelimiter.web.core;

import java.util.Collections;
import java.util.List;

public interface RequestPathPatterns<REQUEST> {

    RequestPathPatterns<Object> NONE = new RequestPathPatterns<Object>() {
        @Override
        public RequestPathPatterns<Object> combine(RequestPathPatterns<Object> other) {
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
    static <T> RequestPathPatterns<T> none() {
        return (RequestPathPatterns<T>)NONE;
    }

    RequestPathPatterns<REQUEST> combine(RequestPathPatterns<REQUEST> other);

    boolean matches(REQUEST request);

    List<String> getPathPatterns();
}
