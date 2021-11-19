package com.looseboxes.ratelimiter.web.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RequestPathPatternsImpl implements RequestPathPatterns<String> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPathPatternsImpl.class);

    private final String [] pathPatterns;

    public RequestPathPatternsImpl(String... pathPatterns) {
        this.pathPatterns = Objects.requireNonNull(pathPatterns);
        LOG.trace("Path patterns: {}", Arrays.toString(pathPatterns));
    }

    public RequestPathPatterns<String> combine(RequestPathPatterns<String> other) {
        final List<String> uris = other.getPathPatterns();
        final int size = uris.size();
        final String [] all = new String[pathPatterns.length * size];
        int k = 0;
        for(int i = 0; i < pathPatterns.length; i++) {
            for(int j = 0; j < size; j++) {
                all[k] = pathPatterns[i] + uris.get(j);
                ++k;
            }
        }
        return new RequestPathPatternsImpl(all);
    }

    @Override
    public boolean matches(String uri) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Checking if: {} matches: {}", uri, Arrays.toString(pathPatterns));
        }
        for(String pathPattern : pathPatterns) {
            if(pathPattern.equals(uri)) {
                LOG.trace("Matches: true, uri: {}, pathPattern: {}", uri, pathPattern);
                return true;
            }
        }
        LOG.trace("Matches: false, uri: {}", uri);
        return false;
    }

    @Override public List<String> getPathPatterns() {
        return Arrays.asList(pathPatterns);
    }

    @Override
    public String toString() {
        return "RequestPathPatternsImpl{" +
                "pathPatterns=" + Arrays.toString(pathPatterns) +
                '}';
    }
}
