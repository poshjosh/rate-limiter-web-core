package com.looseboxes.ratelimiter.web.core;

public interface Registry<T> {

    Registry<T> register(T what);

    Registry<T> register(String name, T what);
}
