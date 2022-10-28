package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface MatcherRegistry<R> {

    MatcherRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher);

    MatcherRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher);

    MatcherRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher);

    /**
     * Get a matcher, which matches all request URIs. The returned matcher is not part of the registry.
     * @return A Matcher which matches all request URIs.
     */
    Matcher<R, String> matchAllUris();

    default Matcher<R, ?> getMatcherOrDefault(String name, @Nullable Object source) {
        if (source instanceof Class) {
            return getOrCreateMatcher(name, (Class)source);
        } else if(source instanceof Method) {
            return getOrCreateMatcher(name, (Method)source);
        } else {
            return getMatcherOrDefault(name, matchAllUris());
        }
    }

    Matcher<R, ?> getOrCreateMatcher(String name, Class<?> clazz);

    Matcher<R, ?> getOrCreateMatcher(String name, Method clazz);

    Matcher<R, ?> getMatcherOrDefault(String name, Matcher<R, ?> resultIfNone);
}
