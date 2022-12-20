package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.util.Matcher;

public interface MatcherRegistry<R> extends Registry<Matcher<R, ?>>{
    Matcher<R, ?> getOrCreateMatcher(String name, @Nullable Object source);
}
