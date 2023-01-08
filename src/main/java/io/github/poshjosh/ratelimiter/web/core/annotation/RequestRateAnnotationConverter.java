package io.github.poshjosh.ratelimiter.web.core.annotation;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.Rate;
import io.github.poshjosh.ratelimiter.annotation.RateGroup;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.MatchConfig;
import io.github.poshjosh.ratelimiter.web.core.util.RequestRates;

public final class RequestRateAnnotationConverter implements AnnotationConverter<Rate, Rates> {

    private final AnnotationConverter<Rate, Rates> delegate = AnnotationConverter.ofRate();

    @Override
    public Class<Rate> getAnnotationType() {
        return Rate.class;
    }

    @Override
    public RequestRates convert(RateGroup rateGroup, Element element, Rate[] rateAnnotations) {
        Rates rates = delegate.convert(rateGroup, element, rateAnnotations);
        return RequestRates.of(rates, getMatchConfig(element));
    }

    public MatchConfig getMatchConfig(Element element) {
        return element.getAnnotation(RateRequestIf.class)
                .map(annotation -> {
                    MatchConfig matchConfig = new MatchConfig();
                    matchConfig.setMatchType(annotation.matchType());
                    matchConfig.setName(annotation.name());
                    matchConfig.setOperator(annotation.operator());
                    matchConfig.setValues(annotation.values());
                    return matchConfig;
                }).orElse(new MatchConfig());
    }
}
