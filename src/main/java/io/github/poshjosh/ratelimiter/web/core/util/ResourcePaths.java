package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.Collections;
import java.util.List;

public interface ResourcePaths {
    ResourcePath<Object> MATCH_ALL = new ResourcePath<Object>() {
        @Override
        public ResourcePath<Object> combine(ResourcePath<Object> other) {
            return other;
        }
        @Override
        public List<String> getPatterns() { return Collections.emptyList(); }
        @Override
        public boolean matches(Object path) {
            return true;
        }
        @Override public String toString() {
            return "ResourcePath$MATCH_ALL";
        }
    };
    ResourcePath<Object> MATCH_NONE = new ResourcePath<Object>() {
        @Override
        public ResourcePath<Object> combine(ResourcePath<Object> other) {
            return other;
        }
        @Override
        public List<String> getPatterns() { return Collections.emptyList(); }
        @Override
        public boolean matches(Object path) {
            return false;
        }
        @Override public String toString() {
            return "ResourcePath$MATCH_NONE";
        }
    };

    @SuppressWarnings("unchecked")
    static <T> ResourcePath<T> matchALL() {
        return (ResourcePath<T>) MATCH_ALL;
    }

    @SuppressWarnings("unchecked")
    static <T> ResourcePath<T> matchNone() {
        return (ResourcePath<T>) MATCH_NONE;
    }
}
