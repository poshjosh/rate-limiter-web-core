package io.github.poshjosh.ratelimiter.web.core.annotation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.Set;

@SupportedAnnotationTypes(RateRequestIfProcessor.ANNOTATION_CLASS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RateRequestIfProcessor extends AbstractProcessor {

    public static final String ANNOTATION_CLASS_NAME = "io.github.poshjosh.ratelimiter.web.core.annotation.RateRequestIf";

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        annotations.forEach(annotation -> {

            Set<? extends Element> annotatedElements
                    = roundEnv.getElementsAnnotatedWith(annotation);

            annotatedElements.forEach(annotatedElement ->{

                RateRequestIf ann = annotatedElement.getAnnotation(RateRequestIf.class);
                if (ann.matchType() == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Must not be null, RateRequestIf.matchType");
                }
            });
        });

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RateRequestIfProcessor.ANNOTATION_CLASS_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_0;
    }
}
