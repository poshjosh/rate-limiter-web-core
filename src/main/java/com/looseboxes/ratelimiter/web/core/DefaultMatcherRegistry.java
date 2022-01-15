package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.web.core.util.ElementPatternsMatcher;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import com.looseboxes.ratelimiter.web.core.util.RequestUriMatcher;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultMatcherRegistry<R> implements MatcherRegistry<R> {

    // TODO - Use shared instances of these. Both classes are used in RateLimiterConfigurationSource
    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final RequestToIdConverter<R, String> requestToUriConverter;

    private final Map<String, Matcher<R, ?>> registeredMatchers;

    private final Map<String, Matcher<R, ?>> sourceElementMatchers;

    private final Matcher<R, ?> matcherForAllRequestUris;

    private final IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;

    private final IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;

    public DefaultMatcherRegistry(
            RequestToIdConverter<R, String> requestToUriConverter,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        this.registeredMatchers = new HashMap<>();
        this.sourceElementMatchers = new HashMap<>();
        this.matcherForAllRequestUris = new RequestUriMatcher<>(requestToUriConverter);
        this.classPathPatternsProvider = Objects.requireNonNull(classPathPatternsProvider);
        this.methodPathPatternsProvider = Objects.requireNonNull(methodPathPatternsProvider);
    }

    @Override public DefaultMatcherRegistry<R> registerRequestMatcher(Class<?> clazz, Matcher<R, ?> matcher) {
        return registerRequestMatcher(classNameProvider.getId(clazz), matcher);
    }

    @Override public DefaultMatcherRegistry<R> registerRequestMatcher(Method method, Matcher<R, ?> matcher) {
        return registerRequestMatcher(methodNameProvider.getId(method), matcher);
    }

    @Override public DefaultMatcherRegistry<R> registerRequestMatcher(String name, Matcher<R, ?> matcher) {
        registeredMatchers.put(name, Objects.requireNonNull(matcher));
        return this;
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcherForProperties(String name) {
        return registeredMatchers.getOrDefault(name, matcherForAllRequestUris);
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcherForSourceElement(String name, Object source) {
        return sourceElementMatchers.computeIfAbsent(name, key -> {
            Matcher<R, ?> registeredMatcher = registeredMatchers.getOrDefault(name, null);
            Matcher<R, ?> sourceElementMatcher = createMatcherForSourceElement(source);
            return registeredMatcher == null ? sourceElementMatcher : sourceElementMatcher.andThen((Matcher)registeredMatcher);
        });
    }

    private Matcher<R, ?> createMatcherForSourceElement(Object source) {
        if(source instanceof Class) {
            return new ElementPatternsMatcher<>((Class<?>)source, classPathPatternsProvider, requestToUriConverter);
        }else if(source instanceof Method) {
            return  new ElementPatternsMatcher<>((Method)source, methodPathPatternsProvider, requestToUriConverter);
        }else{
            throw new UnsupportedOperationException();
        }
    }
}
