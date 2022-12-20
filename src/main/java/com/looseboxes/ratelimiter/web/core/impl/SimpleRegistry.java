package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.web.core.Registry;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class SimpleRegistry<T> implements Registry<T> {

    public static <T> Registry<T> of(T defaultInstance) {
        return of(defaultInstance, IdProvider.forClass(), IdProvider.forMethod());
    }

    public static <T> Registry<T> of(
            T defaultInstance,
            IdProvider<Class<?>, String> classIdProvider,
            IdProvider<Method, String> methodIdProvider) {
        return new SimpleRegistry<>(defaultInstance, classIdProvider, methodIdProvider);
    }

    private final IdProvider<Class<?>, String> classIdProvider;
    private final IdProvider<Method, String> methodIdProvider;
    
    private final Map<String, T> registered;

    private T defaultInstance;

    SimpleRegistry(T defaultInstance,
            IdProvider<Class<?>, String> classIdProvider,
            IdProvider<Method, String> methodIdProvider) {
        this.classIdProvider = Objects.requireNonNull(classIdProvider);
        this.methodIdProvider = Objects.requireNonNull(methodIdProvider);
        this.registered = new ConcurrentHashMap<>();
        this.defaultInstance = Objects.requireNonNull(defaultInstance);
    }

    @Override public Registry<T> register(T rateLimiterFactory) {
        defaultInstance = Objects.requireNonNull(rateLimiterFactory);
        return this;
    }

    @Override public Registry<T> register(Class<?> clazz, T rateLimiterFactory) {
        return register(classIdProvider.getId(clazz), rateLimiterFactory);
    }

    @Override public Registry<T> register(Method method, T rateLimiterFactory) {
        return register(methodIdProvider.getId(method), rateLimiterFactory);
    }

    @Override public Registry<T> register(String name, T rateLimiterFactory) {
        registered.put(name, Objects.requireNonNull(rateLimiterFactory));
        return this;
    }

    @Override public T getOrDefault(Class<?> name, T resultIfNone) {
        return getOrDefault(classIdProvider.getId(name), resultIfNone);
    }

    @Override public T getOrDefault(Method name, T resultIfNone) {
        return getOrDefault(methodIdProvider.getId(name), resultIfNone);
    }

    @Override public T getOrDefault(String name, T resultIfNone) {
        return registered.getOrDefault(name, resultIfNone);
    }

    @Override public T getDefault() {
        return this.defaultInstance;
    }

    Map<String, T> getRegistered() {
        return registered;
    }
}
