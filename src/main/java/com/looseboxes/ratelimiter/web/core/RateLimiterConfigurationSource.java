package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ClassNameProvider;
import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.annotation.MethodNameProvider;
import com.looseboxes.ratelimiter.cache.InMemoryRateCache;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class RateLimiterConfigurationSource<R> implements RateLimiterConfigurationRegistry<R> {

    private final IdProvider<Class<?>, String> classNameProvider = new ClassNameProvider();
    private final IdProvider<Method, String> methodNameProvider = new MethodNameProvider();

    private final RequestToIdConverter<R, String> requestToUriConverter;

    private final Map<String, RequestToIdConverter<R, Object>> converters;

    private RequestToIdConverter<R, Object> defaultRequestToIdConverter;

    private final Map<String, RateLimiterConfiguration<Object>> configurationMap;

    private final RateLimiterConfiguration<Object> defaultConfiguration;

    private RateRecordedListener rootRateRecordedListener;

    private final IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider;

    private final IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider;

    private final Matcher<R> matchAllForRequestUri;

    public RateLimiterConfigurationSource(
            RequestToIdConverter<R, String> requestToUriConverter,
            RateCache<Object> rateCache,
            RateFactory rateFactory,
            RateRecordedListener rateRecordedListener,
            RateLimiterConfigurer<R> rateLimiterConfigurer,
            IdProvider<Class<?>, PathPatterns<String>> classPathPatternsProvider,
            IdProvider<Method, PathPatterns<String>> methodPathPatternsProvider) {
        this.requestToUriConverter = Objects.requireNonNull(requestToUriConverter);
        this.converters = new HashMap<>();
        this.defaultRequestToIdConverter = request -> request;
        this.configurationMap = new HashMap<>();
        this.defaultConfiguration = new RateLimiterConfiguration<>()
                .rateCache(rateCache == null ? new InMemoryRateCache<>() : rateCache)
                .rateFactory(rateFactory == null ? new LimitWithinDurationFactory() : rateFactory)
                .rateRecordedListener(rateRecordedListener == null ? new RateExceededExceptionThrower() : rateRecordedListener);
        this.rootRateRecordedListener = RateRecordedListener.NO_OP;
        if(rateLimiterConfigurer != null) {
            rateLimiterConfigurer.configure(this);
        }
        this.classPathPatternsProvider = Objects.requireNonNull(classPathPatternsProvider);
        this.methodPathPatternsProvider = Objects.requireNonNull(methodPathPatternsProvider);
        this.matchAllForRequestUri = new Matcher<R>() {
            @Override
            public boolean matches(R target) {
                return true;
            }
            @Override
            public Object getId(R target) {
                return requestToUriConverter.convert(target);
            }
        };
    }

    public Matcher<R> getMatcherForProperties(String name) {
        return matchAllForRequestUri;
    }

    public Matcher<R> getMatcherForSource(String name, Object source) {
        if(source instanceof List) {
            List<Matcher<R>> matchers = ((List<?>)source).stream().map(s -> getMatcherForSource(name, s)).collect(Collectors.toList());
            List<Object> matcherIds = matchers.stream().map(matcher -> matcher.getId(null)).collect(Collectors.toList());
            return new Matcher<R>() {
                @Override
                public boolean matches(R target) {
                    return matchers.stream().anyMatch(matcher -> matcher.matches(target));
                }
                @Override
                public Object getId(R target) {
                    return matcherIds;
                }
                @Override
                public String toString() {
                    return "$Matcher{" + matcherIds + "}";
                }
            };
        }else{
            final PathPatterns<String> pathPatterns;
            if(source instanceof Class) {
                pathPatterns = classPathPatternsProvider.getId((Class<?>) source);
            }else if(source instanceof Method) {
                pathPatterns = methodPathPatternsProvider.getId((Method) source);
            }else{
                throw new UnsupportedOperationException();
            }
            return new Matcher<R>() {
                @Override
                public boolean matches(R target) {
                    String uri = requestToUriConverter.convert(target);
                    return pathPatterns.matches(uri);
                }
                @Override
                public Object getId(R target) {
                    return pathPatterns.getPatterns();
                }
                @Override
                public String toString() {
                    return "$Matcher{" + pathPatterns + "}";
                }
            };
        }
    }

    @Override public void registerRequestToIdConverter(RequestToIdConverter<R, Object> requestToIdConverter) {
        defaultRequestToIdConverter = Objects.requireNonNull(requestToIdConverter);
    }

    @Override public void registerRequestToIdConverter(Class<?> clazz,
            RequestToIdConverter<R, Object> requestToIdConverter) {
        registerRequestToIdConverter(classNameProvider.getId(clazz), requestToIdConverter);
    }

    @Override public void registerRequestToIdConverter(Method method,
            RequestToIdConverter<R, Object> requestToIdConverter) {
        registerRequestToIdConverter(methodNameProvider.getId(method), requestToIdConverter);
    }

    @Override public void registerRequestToIdConverter(String name,
            RequestToIdConverter<R, Object> requestToIdConverter) {
        converters.put(name, Objects.requireNonNull(requestToIdConverter));
    }

    // @TODO make private
    public RequestToIdConverter<R, String> getRequestToUriConverter() {
        return requestToUriConverter;
    }

    public RequestToIdConverter<R, Object> getRequestToIdConverterOrDefault(String name) {
        return converters.getOrDefault(name, defaultRequestToIdConverter == null ? requestToUriConverter::convert : defaultRequestToIdConverter);
    }

    @Override public void registerRateCache(RateCache<Object> rateCache) {
        defaultConfiguration.rateCache(Objects.requireNonNull(rateCache));
    }

    @Override public void registerRateCache(Class<?> clazz, RateCache<Object> rateCache) {
        registerRateCache(classNameProvider.getId(clazz), rateCache);
    }

    @Override public void registerRateCache(Method method, RateCache<Object> rateCache) {
        registerRateCache(methodNameProvider.getId(method), rateCache);
    }

    @Override public void registerRateCache(String name, RateCache<Object> rateCache) {
        getOrCreateConfigurationWithDefaults(name).rateCache(Objects.requireNonNull(rateCache));
    }

    @Override public void registerRateFactory(RateFactory rateFactory) {
        defaultConfiguration.rateFactory(Objects.requireNonNull(rateFactory));
    }

    @Override public void registerRateFactory(Class<?> clazz, RateFactory rateFactory) {
        registerRateFactory(classNameProvider.getId(clazz), rateFactory);
    }

    @Override public void registerRateFactory(Method method, RateFactory rateFactory) {
        registerRateFactory(methodNameProvider.getId(method), rateFactory);
    }

    @Override public void registerRateFactory(String name, RateFactory rateFactory) {
        getOrCreateConfigurationWithDefaults(name).setRateFactory(Objects.requireNonNull(rateFactory));
    }

    @Override public void registerRateRecordedListener(RateRecordedListener rateRecordedListener) {
        defaultConfiguration.rateRecordedListener(Objects.requireNonNull(rateRecordedListener));
    }

    @Override public void registerRateRecordedListener(Class<?> clazz, RateRecordedListener rateRecordedListener) {
        registerRateRecordedListener(classNameProvider.getId(clazz), rateRecordedListener);
    }

    @Override public void registerRateRecordedListener(Method method, RateRecordedListener rateRecordedListener) {
        registerRateRecordedListener(methodNameProvider.getId(method), rateRecordedListener);
    }

    @Override public void registerRateRecordedListener(String name, RateRecordedListener rateRecordedListener) {
        getOrCreateConfigurationWithDefaults(name).setRateRecordedListener(Objects.requireNonNull(rateRecordedListener));
    }

    /**
     * Register a root listener, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public void registerRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        rootRateRecordedListener = Objects.requireNonNull(rateRecordedListener);
    }

    /**
     * Add this listener to the root listeners, which will always be invoked before any other listener
     * @param rateRecordedListener The listener to register
     */
    @Override public void addRootRateRecordedListener(RateRecordedListener rateRecordedListener) {
        Objects.requireNonNull(rateRecordedListener);
        rootRateRecordedListener = rootRateRecordedListener.andThen(rateRecordedListener);
    }

    public RateLimiterConfiguration<Object> copyConfigurationOrDefault(String name) {
        RateLimiterConfiguration<Object> result = new RateLimiterConfiguration<>(
                configurationMap.getOrDefault(name, defaultConfiguration));
        if(rootRateRecordedListener != RateRecordedListener.NO_OP) {
            result.setRateRecordedListener(rootRateRecordedListener.andThen(result.getRateRecordedListener()));
        }
        return result;
    }

    private RateLimiterConfiguration<Object> getOrCreateConfigurationWithDefaults(String name) {
        return configurationMap.computeIfAbsent(name, (s) -> new RateLimiterConfiguration<>(defaultConfiguration));
    }
}
