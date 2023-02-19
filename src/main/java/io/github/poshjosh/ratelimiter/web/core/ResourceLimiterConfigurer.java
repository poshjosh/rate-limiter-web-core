package io.github.poshjosh.ratelimiter.web.core;

/**
 * For find-grained configuration of rate limiting.
 *
 * @see #configure(Registries)
 */
@FunctionalInterface
public interface ResourceLimiterConfigurer {

    /**
     * Implement this method for find-grained configuration of rate limiting.
     *
     * Users do not need to directly call the <code>configure(Registries)</code> method.
     * The method is called during setup.
     *
     * <p>Below is some basic examples of configuring rate limiting</p>
     * <pre>
     * <code>
     * @Component
     * public class MyResourceLimiterConfigurer implements ResourceLimiterConfigurer{
     *   public void configure(Registries registries) {
     *     // Register consumption listeners
     *     // ------------------------------
     *
     *     registries.registerListener((context, resourceId, hits, limit) -> {
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
     *     // Rate limit users from a specific utm_source e.g. facebook
     *     registries.matchers().register("limitByUtmSourceIsFacebook",
     *             request -> "facebook".equals(request.getParameter("utm_source")));
     *
     *     // You could use a variety of Cache flavours
     *     // -----------------------------------------
     *
     *     javax.cache.Cache javaxCache = null; // PROVIDE THIS
     *     registries.registerStore(new BandwidthsStoreForCache(cache));
     *   }
     *
     *   private static final class BandwidthsStoreForCache<K> implements BandwidthsStore<K> {
     *     private final javax.cache.Cache<K, Bandwidth> cache;
     *     public BandwidthsStoreForCache(javax.cache.Cache<K, Bandwidth> cache) {
     *       this.cache = cache;
     *     }
     *     @Override public Bandwidth get(K key) {
     *       return cache.get(key);
     *     }
     *     @Override public void put(K key, Bandwidth bandwidth) {
     *       cache.put(key, bandwidth);
     *     }
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
