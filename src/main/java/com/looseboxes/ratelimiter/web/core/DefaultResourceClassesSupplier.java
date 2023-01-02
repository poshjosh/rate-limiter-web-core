package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.ClassFilterForAnnotations;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.web.core.ResourceClassesSupplier;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

final class DefaultResourceClassesSupplier implements ResourceClassesSupplier {

    private final List<Class<?>> classes;

    @SafeVarargs
    DefaultResourceClassesSupplier(
            ClassesInPackageFinder classesInPackageFinder,
            List<String> resourcePackages,
            Class<? extends Annotation>... annotations) {
        if(resourcePackages == null || resourcePackages.isEmpty()) {
            classes = Collections.emptyList();
        }else{
            classes = classesInPackageFinder.findClasses(resourcePackages, new ClassFilterForAnnotations(annotations));
        }
    }

    @Override
    public List<Class<?>> get() {
        return classes;
    }
}
