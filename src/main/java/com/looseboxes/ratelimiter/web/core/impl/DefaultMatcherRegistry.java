package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.web.core.MatcherRegistry;
import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;
import com.looseboxes.ratelimiter.web.core.util.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultMatcherRegistry<R> implements MatcherRegistry<R> {

    private static final class MatcherCreatorForAnnotatedElement<T, E> {

        private final Map<String, Matcher<T, ?>> registeredMatchers;
        private final IdProvider<E, PathPatterns<String>> pathPatternsProvider;
        private final RequestToIdConverter<T, String> requestToUriConverter;

        public MatcherCreatorForAnnotatedElement(Map<String, Matcher<T, ?>> registeredMatchers,
                IdProvider<E, PathPatterns<String>> pathPatternsProvider,
                RequestToIdConverter<T, String> requestToUriConverter) {
            this.registeredMatchers = Objects.requireNonNull(registeredMatchers);
            this.pathPatternsProvider = Objects.requireNonNull(pathPatternsProvider);
            this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        }

        public Matcher<T, ?> createMatcher(String name, E source) {
            Matcher<T, ?> registeredMatcher = registeredMatchers.getOrDefault(name, null);
            Matcher<T, ?> sourceElementMatcher = createMatcher(source);
            return registeredMatcher == null ? sourceElementMatcher : sourceElementMatcher.andThen((Matcher)registeredMatcher);
        }

        private Matcher<T, ?> createMatcher(E source) {
            PathPatterns<String> pathPatterns = pathPatternsProvider.getId(source);
            return new PathPatternsMatcher<>(pathPatterns, requestToUriConverter);
        }
    }

    // TODO - Use shared instances of these. Both classes are used in DefaultRateLimiterRegistry
    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final Map<String, Matcher<R, ?>> registeredMatchers;

    private final Map<String, Matcher<R, ?>> sourceElementMatchers;

    private final Matcher<R, String> matcherForAllRequestUris;

    private final MatcherCreatorForAnnotatedElement<R, Class<?>> classMatcherCreator;
    private final MatcherCreatorForAnnotatedElement<R, Method> methodMatcherCreator;

    public DefaultMatcherRegistry(
            RequestToIdConverter<R, String> requestToUriConverter,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        this.registeredMatchers = new HashMap<>();
        this.sourceElementMatchers = new HashMap<>();
        this.matcherForAllRequestUris = new RequestUriMatcher<>(requestToUriConverter);
        this.classMatcherCreator = new MatcherCreatorForAnnotatedElement<>(
            registeredMatchers, classPathPatternsProvider, requestToUriConverter
        );
        this.methodMatcherCreator = new MatcherCreatorForAnnotatedElement<>(
                registeredMatchers, methodPathPatternsProvider, requestToUriConverter
        );
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

    /**
     * Get a matcher, which matches all request URIs. The returned matcher is not part of the registry.
     * @return A Matcher which matches all request URIs.
     */
    @Override
    public Matcher<R, String> matchAllUris() {
        return matcherForAllRequestUris;
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcher(String name, Class<?> source) {
        return sourceElementMatchers.computeIfAbsent(name, key -> classMatcherCreator.createMatcher(name, source));
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcher(String name, Method source) {
        return sourceElementMatchers.computeIfAbsent(name, key -> methodMatcherCreator.createMatcher(name, source));
    }

    @Override
    public Matcher<R, ?> getMatcherOrDefault(String name, Matcher<R, ?> resultIfNone) {
        return registeredMatchers.getOrDefault(name, resultIfNone);
    }
}
