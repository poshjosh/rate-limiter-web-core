package io.github.poshjosh.ratelimiter.web.core;

/**
 * For fine-grained configuration of rate limiting.
 *
 * @see #configure(Registries)
 */
@FunctionalInterface
public interface RateLimiterConfigurer {

    /**
     * Implement this method for fine-grained configuration of rate limiting.
     *
     * Users do not need to directly call the <code>configure(Registries)</code> method.
     * The method is called during setup.
     *
     * <p>Below is some basic examples of configuring rate limiting</p>
     * <pre>
     * <code>
     * @Component
     * public class MyRateLimiterConfigurer implements RateLimiterConfigurer{
     *   public void configure(Registries registries) {
     *
     *     // Register request matchers
     *     // -------------------------
     *
     *     // Identify resources to rate-limit by session id
     *     registries.matchers().register("limitBySession", request -> request.getSession().getId());
     *
     *     // Identify resources to rate-limit by the presence of request parameter "utm_source"
     *     registries.matchers().register("limitByUtmSource", request -> request.getParameter("utm_source"));
     *
     *     // Rate limit users from a specific utm_source e.g. facebook
     *     registries.matchers().register("limitByUtmSourceIsFacebook",
     *             request -> "facebook".equals(request.getParameter("utm_source")));
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param registries provide means for registering various objects used for configuring
     *                   rate limiting
     */
    void configure(Registries registries);
}
