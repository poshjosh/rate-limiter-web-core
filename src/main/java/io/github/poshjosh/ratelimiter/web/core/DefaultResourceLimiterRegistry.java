package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class DefaultResourceLimiterRegistry implements ResourceLimiterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceLimiterRegistry.class);

    private static class RateConfigCollector implements RateProcessor.NodeConsumer {
        private final Map<String, RateConfig> nameToRateMap;
        public RateConfigCollector() {
            this.nameToRateMap = new HashMap<>();
        }
        @Override
        public void accept(Object genericDeclaration, Node<RateConfig> node) {
            node.getValueOptional().ifPresent(rateConfig -> {
                nameToRateMap.putIfAbsent(node.getName(), rateConfig);
            });
        }
        public Optional<RateConfig> get(String name) {
            return Optional.ofNullable(nameToRateMap.get(name));
        }
    }

    private final Registries registries;
    private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
    private final RateLimitProperties properties;
    private final MatcherProvider matcherProvider;
    private final Node<RateConfig> propertiesRootNode;
    private final Node<RateConfig> annotationsRootNode;

    DefaultResourceLimiterRegistry(ResourceLimiterConfig resourceLimiterConfig) {
        registries = Registries.ofDefaults();
        properties = resourceLimiterConfig.getProperties();
        matchers = new HashMap<>();
        matcherProvider = new MatcherProviderMultiSource(
                resourceLimiterConfig.getMatcherProvider(), registries.matchers(), matchers);

        resourceLimiterConfig.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = resourceLimiterConfig.getPropertyRateProcessor()
                .process(Node.of("root.properties"), propertyConfigs, properties);

        Node<RateConfig> annoRoot = resourceLimiterConfig.getClassRateProcessor()
                .processAll(Node.of("root.annotations"), (src, node) -> {}, resourceLimiterConfig.getResourceClasses());

        List<String> transferredToAnnotations = new ArrayList<>();
        Function<Node<RateConfig>, RateConfig> overrideWithPropertyValue = node -> {
            if (node.isRoot()) {
                return node.getValueOrDefault(null);
            }
            RateConfig annotationConfig = Checks.requireNodeValue(node);
            return propertyConfigs.get(node.getName())
                    .map(propertyConfig -> {
                        transferredToAnnotations.add(node.getName());
                        return propertyConfig.withSource(annotationConfig.getSource());
                    }).orElse(annotationConfig);
        };

        annoRoot = annoRoot.transform(overrideWithPropertyValue);

        Predicate<Node<RateConfig>> isNodeRateLimited = node -> {
            if (node.isRoot()) {
                return true;
            }
            return transferredToAnnotations.contains(node.getName()) || node.getValueOptional()
                    .map(RateConfig::getSource)
                    .filter(RateSource::isRateLimited).isPresent();
        };

        Predicate<Node<RateConfig>> anyNodeInTreeIsRateLimited = node -> {
            return testTree(node, isNodeRateLimited);
        };

        annotationsRootNode = annoRoot.retainAll(anyNodeInTreeIsRateLimited)
                .orElseGet(() -> Node.of("root.annotations"));

        LOG.debug("Nodes:\n{}", annotationsRootNode);

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Node.of("root.properties"));

        LOG.debug("Nodes:\n{}", propertiesRootNode);
    }

    private boolean testTree(Node<RateConfig> node, Predicate<Node<RateConfig>> test) {
        return test.test(node) || node.getChildren().stream().anyMatch(child -> testTree(child, test));
    }

    @Override
    public ResourceLimiter<HttpServletRequest> createResourceLimiter() {

        ResourceLimiter<HttpServletRequest> limiterForProperties = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                (BandwidthsStore)registries.getStoreOrDefault(),
                matcherProvider, propertiesRootNode
        );

        ResourceLimiter<HttpServletRequest> limiterForAnnotations = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                (BandwidthsStore)registries.getStoreOrDefault(),
                matcherProvider, annotationsRootNode
        );

        return new ResourceLimiterWrapper(
                limiterForProperties.andThen(limiterForAnnotations), this::isRateLimitingEnabled);
    }

    @Override
    public List<RateLimiter> createRateLimiters(String id) {
        return getRateConfig(id)
                .map(rateConfig -> createRateLimiters(id, rateConfig.getRates()))
                .orElse(Collections.emptyList());
    }

    private List<RateLimiter> createRateLimiters(String id, Rates rates) {
        RateToBandwidthConverter converter = RateToBandwidthConverter.ofDefaults();
        Bandwidth[] bandwidths = converter.convert(id, rates, 0);
        return Arrays.stream(bandwidths).map(RateLimiter::of).collect(Collectors.toList());
    }

    @Override
    public Optional<RateConfig> getRateConfig(String id) {
        RateConfig rateConfig = propertiesRootNode.findFirstChild(node -> matches(id, node))
                .flatMap(Node::getValueOptional)
                .orElseGet(() -> annotationsRootNode.findFirstChild(node -> matches(id, node))
                        .flatMap(Node::getValueOptional).orElse(null));
        return Optional.ofNullable(rateConfig);
    }

    @Override
    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> matches(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> matches(id, node)).isPresent();
    }

    private boolean matches(String id, Node<RateConfig> node) {
        return id.equals(node.getName());
    }

    public RateLimitProperties properties() {
        return properties;
    }

    /**
     * Return the Matchers registered to {@link Registries#matchers()}
     *
     * Registration is done by calling any of the <code>register</code> methods of the returned
     * {@link io.github.poshjosh.ratelimiter.web.core.Registry}
     *
     * @return The registered matchers
     * @see #getMatchers(String)
     */
    @Override
    public UnmodifiableRegistry<Matcher<HttpServletRequest>> matchers() {
        return Registry.unmodifiable(registries.matchers());
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     * @see #matchers()
     */
    @Override
    public List<Matcher<HttpServletRequest>> getMatchers(String id) {
        List<Matcher<HttpServletRequest>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }

    @Override
    public Optional<BandwidthsStore<?>> getStore() {
        return registries.getStore();
    }

    @Override
    public Optional<UsageListener> getListener() {
        return registries.getListener();
    }

    private static final class ResourceLimiterWrapper implements ResourceLimiter<HttpServletRequest> {
        private final ResourceLimiter<HttpServletRequest> delegate;
        private final BooleanSupplier isEnabled;
        private ResourceLimiterWrapper(ResourceLimiter<HttpServletRequest> delegate, BooleanSupplier isEnabled) {
            this.delegate = Objects.requireNonNull(delegate);
            this.isEnabled = Objects.requireNonNull(isEnabled);
        }
        @Override
        public ResourceLimiter<HttpServletRequest> listener(UsageListener listener) {
            return delegate.listener(listener);
        }
        @Override
        public UsageListener getListener() {
            return delegate.getListener();
        }
        @Override
        public boolean tryConsume(HttpServletRequest key, int permits, long timeout, TimeUnit unit) {
            if (isEnabled.getAsBoolean()) {
                return delegate.tryConsume(key, permits, timeout, unit);
            }
            return true;
        }
    }

    private static final class MatcherProviderMultiSource implements MatcherProvider<HttpServletRequest>{
        private static final Logger LOG = LoggerFactory.getLogger(MatcherProviderMultiSource.class);
        private final MatcherProvider<HttpServletRequest> delegate;
        private final Registry<Matcher<HttpServletRequest>> registry;
        private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
        private MatcherProviderMultiSource(
                MatcherProvider<HttpServletRequest> delegate,
                Registry<Matcher<HttpServletRequest>> registry,
                Map<String, List<Matcher<HttpServletRequest>>> matchers) {
            this.delegate = Objects.requireNonNull(delegate);
            this.registry = Objects.requireNonNull(registry);
            this.matchers = Objects.requireNonNull(matchers);
        }
        @Override
        public Matcher<HttpServletRequest> createMatcher(Node<RateConfig> node) {

            String nodeName = node.getName();

            // If no Matcher or a NO_OP Matcher exists, create new
            Matcher<HttpServletRequest> existing = registry.get(nodeName).orElse(Matcher.matchNone());

            Matcher<HttpServletRequest> created = delegate.createMatcher(node);

            if (Matcher.matchNone().equals(existing)) {
                addIfAbsent(nodeName, created);
                return created;
            } else {
                LOG.debug("Found existing matcher for {}, matcher: {}", nodeName, existing);
                Matcher<HttpServletRequest> result = created == null ? existing : created.andThen(existing);
                addIfAbsent(nodeName, result);
                return result;
            }
        }
        @Override
        public List<Matcher<HttpServletRequest>> createMatchers(Node<RateConfig> node) {
            List<Matcher<HttpServletRequest>> result = delegate.createMatchers(node);
            result.forEach(matcher -> addIfAbsent(node.getName(), matcher));
            return result;
        }
        private void addIfAbsent(String name,  Matcher<HttpServletRequest> matcher) {
            List<Matcher<HttpServletRequest>> list = matchers.computeIfAbsent(name, k -> new ArrayList<>());
            if (!list.contains(matcher)) {
                list.add(matcher);
            }
        }
    }
}
