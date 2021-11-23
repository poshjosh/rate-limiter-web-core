package com.looseboxes.ratelimiter.web.core;

import java.util.List;
import java.util.function.Supplier;

@FunctionalInterface
public interface ResourceClassesSupplier extends Supplier<List<Class<?>>> {

}
