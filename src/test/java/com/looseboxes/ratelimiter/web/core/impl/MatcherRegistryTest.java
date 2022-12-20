package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.IdProvider;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.web.core.AbstractRegistryTest;
import com.looseboxes.ratelimiter.web.core.Registry;
import com.looseboxes.ratelimiter.web.core.util.PathPatterns;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class MatcherRegistryTest extends AbstractRegistryTest<Matcher<String, ?>> {

    private static final class MatcherImpl implements Matcher<String, String>{
        @Override
        public String matchOrNull(String target) {
            return target;
        }
    }

    @Test
    void shouldRegister() {
        super.shouldRegister(new MatcherImpl());
    }

    @Test
    void shouldRegisterByName() {
        super.shouldRegisterByName(new MatcherImpl());
    }

    protected Registry<Matcher<String, ?>> getInstance() {
        return new DefaultMatcherRegistry<>(Object::toString,
                IdProvider.forClass(), IdProvider.forMethod(), this::of, this::of);
    }

    private PathPatterns<String> of(Class<?> clazz) {
        List<String> list = new ArrayList<>();
        list.add(clazz.getName());
        return of(list);
    }

    private PathPatterns<String> of(Method method) {
        List<String> list = new ArrayList<>();
        list.add(method.getName());
        return of(list);
    }

    private PathPatterns<String> of(List<String> list) {
        return new PathPatterns<String>() {
            @Override
            public PathPatterns<String> combine(PathPatterns<String> other) {
                List<String> newList = new ArrayList<>();
                newList.addAll(getPatterns());
                newList.addAll(other.getPatterns());
                return of(newList);
            }
            @Override public List<String> getPatterns() {
                return list;
            }
            @Override public boolean matches(String s) {
                return list.contains(s);
            }
        };
    }
}
