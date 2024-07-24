package io.github.poshjosh.ratelimiter.web.core.util;

import io.github.poshjosh.ratelimiter.model.RateSource;

public interface ResourceInfoProvider {

    ResourceInfoProvider NONE = source -> ResourceInfos.NONE;

    ResourceInfo get(RateSource source);
}
