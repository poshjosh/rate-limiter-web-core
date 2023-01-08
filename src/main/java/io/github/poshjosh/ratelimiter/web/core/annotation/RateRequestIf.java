package io.github.poshjosh.ratelimiter.web.core.annotation;

import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.web.core.util.MatchType;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
public @interface RateRequestIf {

    MatchType matchType() default MatchType.NOOP;

    Operator operator() default Operator.AND;

    String name() default "";

    String[] values() default {};
}
