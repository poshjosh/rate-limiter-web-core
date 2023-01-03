package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class DefaultResourceClassesSupplier implements ResourceClassesSupplier {

    private static final class ClassFilterForAnnotations implements ClassesInPackageFinder.ClassFilter {

        private final List<Class<? extends Annotation>> annotationClassList;

        @SafeVarargs
        public ClassFilterForAnnotations(Class<? extends Annotation>... annotationClasses) {
            this(Arrays.asList(annotationClasses));
        }

        public ClassFilterForAnnotations(List<Class<? extends Annotation>> annotationClasses) {
            this.annotationClassList = Collections.unmodifiableList(new ArrayList<>(annotationClasses));
        }

        @Override
        public boolean test(Class<?> clazz) {
            return annotationClassList.stream()
                    .anyMatch(annotationClass -> hasAnnotation(clazz, annotationClass));
        }

        private boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {

            do {

                if(clazz.isAnnotationPresent(annotationClass)) {
                    return true;
                }

                clazz = clazz.getSuperclass();

            }while(clazz != null && !clazz.equals(Object.class));

            return false;
        }
    }

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
