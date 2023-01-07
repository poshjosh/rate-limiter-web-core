package io.github.poshjosh.ratelimiter.web.core;

/**
 * For find-grained configuration of rate limiting.
 * <br/>
 * <pre>
 * <code>
 * @Component
 * public class MyResourceLimiterConfigurer implements ResourceLimiterConfigurer<HttpServletRequest>{
 *   public void configure(Registries<HttpServletRequest> registries) {
 *     // Register consumption listeners
 *     // ------------------------------
 *
 *     registries.listeners().register((context, resourceId, hits, limit) -> {
 *
 *       // For example, log the limit that was exceeded
 *       System.out.println("For " + resourceId + ", the following limits are exceeded: " + limit);
 *     });
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
 *     // Rate limit users from a specific utm_source e.g facebook
 *     registries.matchers().register("limitByUtmSourceIsFacebook",
 *             request -> "facebook".equals(request.getParameter("utm_source")));
 *
 *     // You could use a variety of Cache flavours
 *     // -----------------------------------------
 *
 *     javax.cache.Cache javaxCache = null; // PROVIDE THIS
 *     registries.caches().register("limitBySession", new JavaRateCache<>(javaxCache));
 *   }
 * }
 * </code>
 * </pre>
 *
 * @param <R> The type of the request
 *           (e.g springframework's HttpServletRequest or JAX-RS' ContainerRequestContext)
 */
@FunctionalInterface
public interface ResourceLimiterConfigurer<R> {
    void configure(Registries<R> registries);
}
