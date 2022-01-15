package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.web.core.util.Matcher;

import java.lang.reflect.Method;

public interface MatcherRegistry<R> {

    MatcherRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher);

    MatcherRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher);

    MatcherRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher);

    Matcher<R, ?> getOrCreateMatcherForProperties(String name);

    Matcher<R, ?> getOrCreateMatcherForSourceElement(String name, Object source);
}
