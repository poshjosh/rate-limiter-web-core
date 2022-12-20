package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.util.Matcher;

import java.lang.reflect.Method;

public interface MatcherRegistry<R> extends Registry<Matcher<R, ?>>{

    default Matcher<R, ?> getMatcherOrDefault(String name, @Nullable Object source) {
        if (source instanceof Class) {
            return getOrCreateMatcher(name, (Class)source);
        } else if(source instanceof Method) {
            return getOrCreateMatcher(name, (Method)source);
        } else {
            return getOrDefault(name);
        }
    }

    Matcher<R, ?> getOrCreateMatcher(String name, Class<?> clazz);

    Matcher<R, ?> getOrCreateMatcher(String name, Method clazz);
}
