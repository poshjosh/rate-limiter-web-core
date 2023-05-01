package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;

final class DefaultResourceLimiterRegistry implements ResourceLimiterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceLimiterRegistry.class);

    private final AnnotationConverter<Rate, Rates> annotationConverter = AnnotationConverter.ofRate();

    private final RateToBandwidthConverter rateToBandwidthConverter = RateToBandwidthConverter.ofDefaults();

    private final Ticker ticker = ResourceLimiter.DEFAULT_TICKER;

    private final Registries registries;
    private final ResourceLimiterConfig resourceLimiterConfig;
    private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
    private final Node<LimiterConfig<HttpServletRequest>> propertiesRootNode;
    private final Node<LimiterConfig<HttpServletRequest>> annotationsRootNode;

    DefaultResourceLimiterRegistry(ResourceLimiterConfig resourceLimiterConfig) {
        this.registries = Registries.ofDefaults();
        this.resourceLimiterConfig = Objects.requireNonNull(resourceLimiterConfig);
        this.matchers = new ConcurrentHashMap<>();

        resourceLimiterConfig.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = resourceLimiterConfig.getPropertyRateProcessor()
                .process(Node.of("root.properties"), propertyConfigs, properties());

        Node<RateConfig> annoRoot = resourceLimiterConfig.getClassRateProcessor()
                .processAll(Node.of("root.annotations"),
                        (src, node) -> {}, resourceLimiterConfig.getResourceClassesSupplier().get());

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

        MatcherProvider<HttpServletRequest> matcherProvider = getRegisteredMatcherProvider();

        annotationsRootNode = annoRoot.retainAll(anyNodeInTreeIsRateLimited)
                .orElseGet(() -> Node.of("root.annotations"))
                .getRoot().transform(rateToLimiterConfig(matcherProvider));

        LOG.debug("Nodes:\n{}", annotationsRootNode);

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Node.of("root.properties"))
                .getRoot().transform(rateToLimiterConfig(matcherProvider));

        LOG.debug("Nodes:\n{}", propertiesRootNode);
    }

    @Override
    public boolean register(Class<?> source) {
        if (isRateLimited(ElementId.of(source))) {
            return false;
        }
        Node<RateConfig> node = createNode(null, source);
        toLimiterConfigNode(annotationsRootNode, node, getRegisteredMatcherProvider());
        return true;
    }

    @Override
    public boolean register(Method source) {
        if (isRateLimited(ElementId.of(source))) {
            return false;
        }
        Node<RateConfig> node = createNode(null, source);
        toLimiterConfigNode(annotationsRootNode, node, getRegisteredMatcherProvider());
        return true;
    }

    @Override
    public Matcher<HttpServletRequest> getOrCreateMatcher(Class<?> clazz) {
        return getUnregisteredMatcherProvider().createMatcher(createRateConfig(clazz));
    }

    @Override
    public Matcher<HttpServletRequest> getOrCreateMatcher(Method method) {
        return getUnregisteredMatcherProvider().createMatcher(createRateConfig(method));
    }

    @Override
    public List<Matcher<HttpServletRequest>> getOrCreateMatchers(Class<?> clazz) {
        return getUnregisteredMatcherProvider().createMatchers(createRateConfig(clazz));
    }

    @Override
    public List<Matcher<HttpServletRequest>> getOrCreateMatchers(Method method) {
        return getUnregisteredMatcherProvider().createMatchers(createRateConfig(method));
    }

    @Override
    public ResourceLimiter<HttpServletRequest> createResourceLimiter() {

        ResourceLimiter<HttpServletRequest> limiterForProperties = createResourceLimiter(propertiesRootNode);

        ResourceLimiter<HttpServletRequest> limiterForAnnotations = createResourceLimiter(annotationsRootNode);

        return limiterForProperties.andThen(limiterForAnnotations);
    }

    @Override
    public ResourceLimiter<HttpServletRequest> createResourceLimiter(Class<?> source) {
        LimiterConfig<HttpServletRequest> config = getConfig(source).orElseGet(() -> createConfig(source));
        return createResourceLimiter(Node.of(config.getId(), config, null));
    }

    @Override
    public ResourceLimiter<HttpServletRequest> createResourceLimiter(Method source) {
        LimiterConfig<HttpServletRequest> config = getConfig(source).orElseGet(() -> createConfig(source));
        return createResourceLimiter(Node.of(config.getId(), config, null));
    }

    @Override
    public List<RateLimiter> createRateLimiters(Class<?> clazz) {
        LimiterConfig<HttpServletRequest> config = getConfig(clazz).orElseGet(() -> createConfig(clazz));
        return createRateLimiters(config.getId(), config.getRates());
    }

    @Override
    public List<RateLimiter> createRateLimiters(Method method) {
        LimiterConfig<HttpServletRequest> config = getConfig(method).orElseGet(() -> createConfig(method));
        return createRateLimiters(config.getId(), config.getRates());
    }

    private List<RateLimiter> createRateLimiters(String id, Rates rates) {
        if (!rates.hasLimits()) {
            return Collections.emptyList();
        }
        Bandwidth[] bandwidths = rateToBandwidthConverter.convert(id, rates, ticker.elapsedMicros());
        return Arrays.stream(bandwidths).map(RateLimiter::of).collect(Collectors.toList());
    }

    @Override
    public LimiterConfig<HttpServletRequest> createConfig(Class<?> source) {
        Node<RateConfig> node = createNode(null, source);
        return toLimiterConfigNode(null, node, getUnregisteredMatcherProvider())
                .getValueOptional().orElseThrow(AssertionError::new);
    }

    @Override
    public LimiterConfig<HttpServletRequest> createConfig(Method source) {
        Node<RateConfig> node = createNode(null, source);
        return toLimiterConfigNode(null, node, getUnregisteredMatcherProvider())
                .getValueOptional().orElseThrow(AssertionError::new);
    }

    @Override
    public Optional<LimiterConfig<HttpServletRequest>> getConfig(String id) {
        LimiterConfig<HttpServletRequest> limiterConfig = propertiesRootNode
                .findFirstChild(node -> isName(id, node))
                .flatMap(Node::getValueOptional)
                .orElseGet(() -> annotationsRootNode.findFirstChild(node -> isName(id, node))
                        .flatMap(Node::getValueOptional).orElse(null));
        return Optional.ofNullable(limiterConfig);
    }

    @Override
    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> isName(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> isName(id, node)).isPresent();
    }

    @Override
    public RateLimitProperties properties() {
        return resourceLimiterConfig.getProperties();
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     */
    @Override
    public List<Matcher<HttpServletRequest>> getMatchers(String id) {
        List<Matcher<HttpServletRequest>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }

    @Override public UnmodifiableRegistries registries() {
        return Registries.unmodifiable(registries);
    }

    private <T> boolean isName(String id, Node<T> node) {
        return id.equals(node.getName());
    }

    private Node<LimiterConfig<HttpServletRequest>> toLimiterConfigNode(
            Node<LimiterConfig<HttpServletRequest>> parent,
            Node<RateConfig> node,
            MatcherProvider<HttpServletRequest> matcherProvider) {
        return Node.of(node.getName(), rateToLimiterConfig(matcherProvider).apply(node), parent);
    }

    private Function<Node<RateConfig>, LimiterConfig<HttpServletRequest>> rateToLimiterConfig(
            MatcherProvider<HttpServletRequest> matcherProvider) {
        return node -> LimiterConfig.of(rateToBandwidthConverter, matcherProvider, ticker, node);
    }

    private boolean testTree(Node<RateConfig> node, Predicate<Node<RateConfig>> test) {
        return test.test(node) || node.getChildren().stream().anyMatch(child -> testTree(child, test));
    }

    private ResourceLimiter<HttpServletRequest> createResourceLimiter(
            Node<LimiterConfig<HttpServletRequest>> node) {
        ResourceLimiter<HttpServletRequest> resourceLimiter = ResourceLimiter.of(
                getUsageListener(node.getName()),
                (RateLimiterProvider)resourceLimiterConfig.getRateLimiterProvider(),
                node);
        return new ResourceLimiterWrapper(resourceLimiter, this::isRateLimitingEnabled);
    }

    private UsageListener getUsageListener(String name) {
        UsageListener global = resourceLimiterConfig.getUsageListener();
        return name == null || name.isEmpty() ? global : registries.listeners().get(name)
                .map(listener -> listener.andThen(global)).orElse(global);
    }

    private Node<RateConfig> createNode(Node<RateConfig> parent, Class<?> source) {
        RateConfig rateConfig = createRateConfig(source);
        return Node.of(rateConfig.getId(), rateConfig, parent);
    }

    private Node<RateConfig> createNode(Node<RateConfig> parent, Method source) {
        RateConfig rateConfig = createRateConfig(source);
        return Node.of(rateConfig.getId(), rateConfig, parent);
    }

    private RateConfig createRateConfig(Class<?> source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(RateSource.of(source), rates);
    }


    private RateConfig createRateConfig(Method source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(RateSource.of(source), rates);
    }

    private MatcherProvider<HttpServletRequest> getUnregisteredMatcherProvider() {
        return getMatcherProvider((name, matcher) -> {});
    }

    private MatcherProvider<HttpServletRequest> getRegisteredMatcherProvider() {
        return getMatcherProvider(new MatcherCollector(matchers));
    }

    private MatcherProvider<HttpServletRequest> getMatcherProvider(
            BiConsumer<String, Matcher<HttpServletRequest>> onMatcherCreated) {
        return new MatcherProviderMultiSource(
                resourceLimiterConfig.getMatcherProvider(), registries.matchers(), onMatcherCreated);
    }

    private static final class RateConfigCollector implements RateProcessor.NodeConsumer {
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

    private static final class MatcherCollector implements BiConsumer<String, Matcher<HttpServletRequest>> {
        private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
        public MatcherCollector(Map<String, List<Matcher<HttpServletRequest>>> matchers) {
            this.matchers = Objects.requireNonNull(matchers);
        }
        @Override public void accept(String name, Matcher<HttpServletRequest> matcher) {
            List<Matcher<HttpServletRequest>> list = matchers.computeIfAbsent(name, k -> new ArrayList<>());
            if (!list.contains(matcher)) {
                list.add(matcher);
            }
        }
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
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ResourceLimiterWrapper that = (ResourceLimiterWrapper) o;
            return delegate.equals(that.delegate);
        }
        @Override public int hashCode() {
            return Objects.hash(delegate);
        }
        @Override public String toString() {
            return delegate.toString();
        }
    }

    private static final class MatcherProviderMultiSource implements MatcherProvider<HttpServletRequest>{
        private static final Logger LOG = LoggerFactory.getLogger(MatcherProviderMultiSource.class);
        private final MatcherProvider<HttpServletRequest> delegate;
        private final Registry<Matcher<HttpServletRequest>> registry;
        private final BiConsumer<String, Matcher<HttpServletRequest>> onMatcherCreated;
        private MatcherProviderMultiSource(
                MatcherProvider<HttpServletRequest> delegate,
                Registry<Matcher<HttpServletRequest>> registry,
                BiConsumer<String, Matcher<HttpServletRequest>> onMatcherCreated) {
            this.delegate = Objects.requireNonNull(delegate);
            this.registry = Objects.requireNonNull(registry);
            this.onMatcherCreated = Objects.requireNonNull(onMatcherCreated);
        }
        @Override
        public Matcher<HttpServletRequest> createMatcher(RateConfig rateConfig) {

            final String id = rateConfig.getId();

            // If no Matcher or a NO_OP Matcher exists, create new
            Matcher<HttpServletRequest> existing = registry.get(id).orElse(Matcher.matchNone());

            Matcher<HttpServletRequest> created = delegate.createMatcher(rateConfig);

            if (Matcher.matchNone().equals(existing)) {
                onMatcherCreated.accept(id, created);
                return created;
            }

            LOG.debug("Found existing matcher for {}, matcher: {}", id, existing);
            Matcher<HttpServletRequest> result = created == null ? existing : created.andThen(existing);
            onMatcherCreated.accept(id, result);
            return result;
        }
        @Override
        public List<Matcher<HttpServletRequest>> createMatchers(RateConfig rateConfig) {
            List<Matcher<HttpServletRequest>> result = delegate.createMatchers(rateConfig);
            result.forEach(matcher -> onMatcherCreated.accept(rateConfig.getId(), matcher));
            return result;
        }
    }
}
