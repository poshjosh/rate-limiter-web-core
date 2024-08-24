package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.matcher.Matcher;
import io.github.poshjosh.ratelimiter.web.core.registry.Registry;

/**
 * For fine-grained configuration of rate limiting.
 */
@FunctionalInterface
public interface RateLimiterConfigurer {

    /**
     * Implement this method for fine-grained configuration of rate limiting.
     * Users do not need to directly call the <code>configureMatchers(Registry<Matcher>)</code>
     * method. The method is called during setup.
     * <p>Below is some basic examples of configuring rate limiting</p>
     * <pre>
     * <code>
     * @org.springframework.stereotype.Component
     * public class MyRateLimiterConfigurer implements RateLimiterConfigurer{
     *   public void configureMatchers(Registry<Matcher<RequestInfo>> matcherRegistry) {
     *
     *     // Register request matchers
     *     // -------------------------
     *
     *     // Identify resources to rate-limit by session id
     *     matcherRegistry.register("limitBySession", request -> request.getSession().getId());
     *
     *     // Identify resources to rate-limit by the presence of request parameter "utm_source"
     *     matcherRegistry.register("limitByUtmSource", request -> request.getParameter("utm_source"));
     *
     *     // Rate limit users from a specific utm_source e.g. facebook
     *     matcherRegistry.register("limitByUtmSourceIsFacebook",
     *             request -> "facebook".equals(request.getParameter("utm_source")));
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param matcherRegistry provide means for registering various Matchers used for configuring
     *                        rate limiting
     */
    void configureMatchers(Registry<Matcher<RequestInfo>> matcherRegistry);
}
