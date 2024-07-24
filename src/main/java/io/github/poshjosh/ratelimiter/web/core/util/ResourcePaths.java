package io.github.poshjosh.ratelimiter.web.core.util;

import java.util.Collections;
import java.util.List;

public interface ResourcePaths {
    ResourcePath MATCH_ALL = new ResourcePath() {
        @Override
        public ResourcePath combine(ResourcePath other) {
            return other;
        }
        @Override
        public List<String> getPatterns() { return Collections.emptyList(); }
        @Override
        public boolean matches(String path) {
            return true;
        }
        @Override public String toString() {
            return "ResourcePath$MATCH_ALL";
        }
    };
    ResourcePath MATCH_NONE = new ResourcePath() {
        @Override
        public ResourcePath combine(ResourcePath other) {
            return other;
        }
        @Override
        public List<String> getPatterns() { return Collections.emptyList(); }
        @Override
        public boolean matches(String path) {
            return false;
        }
        @Override public String toString() {
            return "ResourcePath$MATCH_NONE";
        }
    };

    static ResourcePath matchALL() {
        return MATCH_ALL;
    }

    static ResourcePath matchNone() {
        return MATCH_NONE;
    }
}
