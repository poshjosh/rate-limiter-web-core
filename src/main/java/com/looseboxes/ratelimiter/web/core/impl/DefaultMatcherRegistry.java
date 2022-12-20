package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Nullable;
import com.looseboxes.ratelimiter.web.core.MatcherRegistry;
import com.looseboxes.ratelimiter.web.core.RequestToIdConverter;
import com.looseboxes.ratelimiter.web.core.util.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class DefaultMatcherRegistry<R> extends SimpleRegistry<Matcher<R, ?>> implements MatcherRegistry<R>{

    private static final class MatcherCreatorForAnnotatedElement<T, E> {

        private final Map<String, Matcher<T, ?>> registeredMatchers;
        private final IdProvider<E, PathPatterns<String>> pathPatternsProvider;
        private final RequestToIdConverter<T, String> requestToUriConverter;

        private MatcherCreatorForAnnotatedElement(
                Map<String, Matcher<T, ?>> registeredMatchers,
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

    private final Map<String, Matcher<R, ?>> sourceElementMatchers;

    private final MatcherCreatorForAnnotatedElement<R, Class<?>> classMatcherCreator;
    private final MatcherCreatorForAnnotatedElement<R, Method> methodMatcherCreator;

    DefaultMatcherRegistry(
            RequestToIdConverter<R, String> requestToUriConverter,
            IdProvider<Class<?>, String> classIdProvider,
            IdProvider<Method, String> methodIdProvider,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        super(new RequestUriMatcher<>(requestToUriConverter), classIdProvider, methodIdProvider);
        this.sourceElementMatchers = new HashMap<>();
        this.classMatcherCreator = new MatcherCreatorForAnnotatedElement<>(
            getRegistered(), classPathPatternsProvider, requestToUriConverter
        );
        this.methodMatcherCreator = new MatcherCreatorForAnnotatedElement<>(
                getRegistered(), methodPathPatternsProvider, requestToUriConverter
        );
    }

    @Override
    public Matcher<R, ?> getOrCreateMatcher(String name, @Nullable Object source) {
        if (source instanceof Class) {
            return getOrCreateMatcher(name, (Class) source);
        } else if (source instanceof Method) {
            return getOrCreateMatcher(name, (Method) source);
        } else {
            return getOrDefault(name);
        }
    }

    private Matcher<R, ?> getOrCreateMatcher(String name, Class<?> source) {
        return sourceElementMatchers.computeIfAbsent(name, key -> createMatcher(key, source));
    }

    private Matcher<R, ?> getOrCreateMatcher(String name, Method source) {
        return sourceElementMatchers.computeIfAbsent(name, key -> createMatcher(key, source));
    }

    private Matcher<R, ?> createMatcher(String name, Class<?> source) {
        return classMatcherCreator.createMatcher(name, source);
    }

    private Matcher<R, ?> createMatcher(String name, Method source) {
        return methodMatcherCreator.createMatcher(name, source);
    }
}
