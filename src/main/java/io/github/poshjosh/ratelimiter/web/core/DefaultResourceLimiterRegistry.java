package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.Element;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

final class DefaultResourceLimiterRegistry<R> implements ResourceLimiterRegistry<R> {

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

    private final Registries<R> registries;
    private final Map<String, List<Matcher<R, ?>>> matchers;
    private final RateLimitProperties properties;
    private final MatcherProvider matcherProvider;
    private final Node<RateConfig> propertiesRootNode;
    private final Node<RateConfig> annotationsRootNode;

    DefaultResourceLimiterRegistry(ResourceLimiterConfig<R> resourceLimiterConfig) {
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
                    .map(val -> (Element)val.getSource())
                    .filter(Element::isRateLimited).isPresent();
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

    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> isRateLimited(id, node)).isPresent();
    }

    private boolean isRateLimited(String id, Node<RateConfig> node) {
        return id.equals(node.getName());
    }

    public ResourceLimiter<R> createResourceLimiter() {

        ResourceLimiter<R> limiterForProperties = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                (BandwidthsStore)registries.getStoreOrDefault(),
                matcherProvider, propertiesRootNode
        );

        ResourceLimiter<R> limiterForAnnotations = ResourceLimiter.of(
                registries.getListenerOrDefault(),
                (BandwidthsStore)registries.getStoreOrDefault(),
                matcherProvider, annotationsRootNode
        );

        return new ResourceLimiterWrapper<>(
                limiterForProperties.andThen(limiterForAnnotations), this::isRateLimitingEnabled);
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
    @Override public UnmodifiableRegistry<Matcher<R, ?>> matchers() {
        return Registry.unmodifiable(registries.matchers());
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     * @see #matchers()
     */
    public List<Matcher<R, ?>> getMatchers(String id) {
        List<Matcher<R, ?>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }

    public Optional<BandwidthsStore<?>> getStore() {
        return registries.getStore();
    }

    public Optional<UsageListener> getListener() {
        return registries.getListener();
    }

    private static final class ResourceLimiterWrapper<R> implements ResourceLimiter<R> {
        private final ResourceLimiter<R> delegate;
        private final BooleanSupplier isEnabled;
        private ResourceLimiterWrapper(ResourceLimiter<R> delegate, BooleanSupplier isEnabled) {
            this.delegate = Objects.requireNonNull(delegate);
            this.isEnabled = Objects.requireNonNull(isEnabled);
        }
        @Override
        public ResourceLimiter<R> listener(UsageListener listener) {
            return delegate.listener(listener);
        }
        @Override
        public UsageListener getListener() {
            return delegate.getListener();
        }
        @Override
        public boolean tryConsume(R key, int permits, long timeout, TimeUnit unit) {
            if (isEnabled.getAsBoolean()) {
                return delegate.tryConsume(key, permits, timeout, unit);
            }
            return true;
        }
    }

    private static final class MatcherProviderMultiSource<R, K> implements MatcherProvider<R, K>{
        private static final Logger LOG = LoggerFactory.getLogger(MatcherProviderMultiSource.class);
        private final MatcherProvider<R, K> delegate;
        private final Registry<Matcher<R, K>> registry;
        private final Map<String, List<Matcher<R, ?>>> matchers;
        private MatcherProviderMultiSource(
                MatcherProvider<R, K> delegate,
                Registry<Matcher<R, K>> registry,
                Map<String, List<Matcher<R, ?>>> matchers) {
            this.delegate = Objects.requireNonNull(delegate);
            this.registry = Objects.requireNonNull(registry);
            this.matchers = Objects.requireNonNull(matchers);
        }
        @Override
        public Matcher<R, K> createMatcher(Node<RateConfig> node) {

            String nodeName = node.getName();

            // If no Matcher or a NO_OP Matcher exists, create new
            Matcher<R, K> existing = registry.get(nodeName).orElse(Matcher.matchNone());

            Matcher<R, K> created = delegate.createMatcher(node);

            if (existing == Matcher.MATCH_NONE) {
                addIfAbsent(nodeName, created);
                return created;
            } else {
                LOG.debug("Found existing matcher for {}, matcher: {}", nodeName, existing);
                BinaryOperator<K> resultComposer = (k0, k1) -> (K)(k0 + "_" + k1);
                Matcher<R, K> result = created == null ? existing : created.andThen(existing, resultComposer);
                addIfAbsent(nodeName, result);
                return result;
            }
        }
        @Override
        public List<Matcher<R, K>> createMatchers(Node<RateConfig> node) {
            List<Matcher<R, K>> result = delegate.createMatchers(node);
            result.forEach(matcher -> addIfAbsent(node.getName(), matcher));
            return result;
        }
        private void addIfAbsent(String name,  Matcher<R, K> matcher) {
            List<Matcher<R, ?>> list = matchers.computeIfAbsent(name, k -> new ArrayList<>());
            if (!list.contains(matcher)) {
                list.add(matcher);
            }
        }
    }
}
