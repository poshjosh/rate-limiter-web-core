package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.*;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.*;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

final class DefaultRateLimiterRegistry implements RateLimiterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRateLimiterRegistry.class);

    private final AnnotationConverter<Rate, Rates> annotationConverter = AnnotationConverter.ofRate();
    private final Registries registries;
    private final RateLimiterContext rateLimiterContext;
    private final Map<String, List<Matcher<HttpServletRequest>>> matchers;
    private final Node<LimiterContext<HttpServletRequest>> propertiesRootNode;
    private final Node<LimiterContext<HttpServletRequest>> annotationsRootNode;

    DefaultRateLimiterRegistry(RateLimiterContext rateLimiterContext) {
        this.registries = Registries.ofDefaults();
        this.rateLimiterContext = Objects.requireNonNull(rateLimiterContext);
        this.matchers = new ConcurrentHashMap<>();

        rateLimiterContext.getConfigurer()
                .ifPresent(configurer -> configurer.configure(registries));

        RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = rateLimiterContext.getPropertyRateProcessor()
                .process(Node.of("root.properties"), propertyConfigs, properties());

        Node<RateConfig> annoRoot = rateLimiterContext.getClassRateProcessor()
                .processAll(Node.of("root.annotations"),
                        (src, node) -> {}, rateLimiterContext.getResourceClassesSupplier().get());

        List<String> transferredToAnnotations = new ArrayList<>();
        Function<Node<RateConfig>, RateConfig> overrideWithPropertyValue = node -> {
            if (node.isRoot()) {
                return node.getValueOrDefault(null);
            }
            RateConfig annotationConfig = node.requireValue();
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
                .getRoot().transform(rateToLimiterContext(matcherProvider));

        LOG.debug("Nodes:\n{}", annotationsRootNode);

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Node.of("root.properties"))
                .getRoot().transform(rateToLimiterContext(matcherProvider));

        LOG.debug("Nodes:\n{}", propertiesRootNode);
    }

    @Override
    public void register(Class<?> source) {
        if (isRateLimited(ElementId.of(source))) {
            throw new IllegalArgumentException("Already registered: " + source);
        }
        Node<RateConfig> node = createNode(null, source);
        toLimiterContextNode(annotationsRootNode, node, getRegisteredMatcherProvider());
    }

    @Override
    public void register(Method source) {
        if (isRateLimited(ElementId.of(source))) {
            throw new IllegalArgumentException("Already registered: " + source);
        }
        Node<RateConfig> node = createNode(null, source);
        toLimiterContextNode(annotationsRootNode, node, getRegisteredMatcherProvider());
    }

    @Override
    public RateLimiterFactory<HttpServletRequest> createRateLimiterFactory() {

        RateLimiterFactory<HttpServletRequest> factoryForProperties =
                createRateLimiterFactory(propertiesRootNode);

        RateLimiterFactory<HttpServletRequest> factoryForAnnotations =
                createRateLimiterFactory(annotationsRootNode);

        return factoryForProperties.andThen(factoryForAnnotations);
    }

    @Override
    public RateLimiterFactory<HttpServletRequest> createRateLimiterFactory(Class<?> source) {
        LimiterContext<HttpServletRequest> context = getLimiterContext(source).orElseGet(() -> createLimiterContext(source));
        return createRateLimiterFactory(Node.of(context.getId(), context, null));
    }

    @Override
    public RateLimiterFactory<HttpServletRequest> createRateLimiterFactory(Method source) {
        LimiterContext<HttpServletRequest> context = getLimiterContext(source).orElseGet(() -> createLimiterContext(source));
        return createRateLimiterFactory(Node.of(context.getId(), context, null));
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Class<?> clazz) {
        LimiterContext<HttpServletRequest> context = getLimiterContext(clazz).orElseGet(() -> createLimiterContext(clazz));
        return getRateLimiter(ElementId.of(clazz), context.getRates());
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Method method) {
        LimiterContext<HttpServletRequest> context = getLimiterContext(method).orElseGet(() -> createLimiterContext(method));
        return getRateLimiter(ElementId.of(method), context.getRates());
    }

    @Override
    public boolean isRateLimited(String id) {
        return propertiesRootNode.findFirstChild(node -> isName(id, node)).isPresent()
                || annotationsRootNode.findFirstChild(node -> isName(id, node)).isPresent();
    }

    @Override
    public boolean hasMatching(String id) {
        return getMatchers(id).stream().anyMatch(matcher -> !Matcher.matchNone().equals(matcher));
    }

    @Override
    public RateLimitProperties properties() {
        return rateLimiterContext.getProperties();
    }

    @Override
    public UnmodifiableRegistries registries() {
        return Registries.unmodifiable(registries);
    }

    private Optional<RateLimiter> getRateLimiter(String key, Rates rates) {
        if (!rates.hasLimits()) {
            return Optional.empty();
        }
        RateLimiterProvider provider = rateLimiterContext.getRateLimiterProvider();
        return Optional.of(provider.getRateLimiter(key, rates));
    }

    private LimiterContext<HttpServletRequest> createLimiterContext(Class<?> source) {
        Node<RateConfig> node = createNode(null, source);
        return toLimiterContextNode(null, node, getUnregisteredMatcherProvider())
                .requireValue();
    }
    private LimiterContext<HttpServletRequest> createLimiterContext(Method source) {
        Node<RateConfig> node = createNode(null, source);
        return toLimiterContextNode(null, node, getUnregisteredMatcherProvider())
                .requireValue();
    }
    private Optional<LimiterContext<HttpServletRequest>> getLimiterContext(Class<?> clazz) {
        return getLimiterContext(ElementId.of(clazz));
    }
    private Optional<LimiterContext<HttpServletRequest>> getLimiterContext(Method method) {
        return getLimiterContext(ElementId.of(method));
    }
    private Optional<LimiterContext<HttpServletRequest>> getLimiterContext(String id) {
        LimiterContext<HttpServletRequest> limiterContext = propertiesRootNode
                .findFirstChild(node -> isName(id, node))
                .flatMap(Node::getValueOptional)
                .orElseGet(() -> annotationsRootNode.findFirstChild(node -> isName(id, node))
                        .flatMap(Node::getValueOptional).orElse(null));
        return Optional.ofNullable(limiterContext);
    }

    private <T> boolean isName(String id, Node<T> node) {
        return id.equals(node.getName());
    }

    private Node<LimiterContext<HttpServletRequest>> toLimiterContextNode(
            Node<LimiterContext<HttpServletRequest>> parent,
            Node<RateConfig> node,
            MatcherProvider<HttpServletRequest> matcherProvider) {
        return Node.of(node.getName(), rateToLimiterContext(matcherProvider).apply(node), parent);
    }

    private Function<Node<RateConfig>, LimiterContext<HttpServletRequest>> rateToLimiterContext(
            MatcherProvider<HttpServletRequest> matcherProvider) {
        return node -> LimiterContext.of(matcherProvider, node);
    }

    private boolean testTree(Node<RateConfig> node, Predicate<Node<RateConfig>> test) {
        return test.test(node) || node.getChildren().stream().anyMatch(child -> testTree(child, test));
    }

    private RateLimiterFactory<HttpServletRequest> createRateLimiterFactory(
            Node<LimiterContext<HttpServletRequest>> node) {
        RateLimiterFactory<HttpServletRequest> rateLimiterFactory = RateLimiterFactory.of(
                node,
                (RateLimiterProvider) rateLimiterContext.getRateLimiterProvider());
        return new RateLimiterFactoryWrapper(rateLimiterFactory, this::isRateLimitingEnabled);
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
        return RateConfig.of(JavaRateSource.of(source), rates);
    }

    private RateConfig createRateConfig(Method source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(JavaRateSource.of(source), rates);
    }

    /**
     * @param id The id of the matchers to return
     * @return All the matchers that will be applied for the given id
     */
    private List<Matcher<HttpServletRequest>> getMatchers(String id) {
        List<Matcher<HttpServletRequest>> result = matchers.get(id);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }


    private Matcher<HttpServletRequest> getOrCreateMainMatcher(Class<?> clazz) {
        return getUnregisteredMatcherProvider().createMainMatcher(createRateConfig(clazz));
    }

    private Matcher<HttpServletRequest> getOrCreateMainMatcher(Method method) {
        return getUnregisteredMatcherProvider().createMainMatcher(createRateConfig(method));
    }

    private List<Matcher<HttpServletRequest>> getOrCreateSubMatchers(Class<?> clazz) {
        return getUnregisteredMatcherProvider().createSubMatchers(createRateConfig(clazz));
    }

    private List<Matcher<HttpServletRequest>> getOrCreateSubMatchers(Method method) {
        return getUnregisteredMatcherProvider().createSubMatchers(createRateConfig(method));
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
                rateLimiterContext.getMatcherProvider(), registries.matchers(), onMatcherCreated);
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

    private static final class RateLimiterFactoryWrapper implements RateLimiterFactory<HttpServletRequest> {
        private final RateLimiterFactory<HttpServletRequest> delegate;
        private final BooleanSupplier isEnabled;
        private RateLimiterFactoryWrapper(
                RateLimiterFactory<HttpServletRequest> delegate,
                BooleanSupplier isEnabled) {
            this.delegate = Objects.requireNonNull(delegate);
            this.isEnabled = Objects.requireNonNull(isEnabled);
        }
        @Override public RateLimiter getRateLimiterOrDefault(
                HttpServletRequest key, RateLimiter resultIfNone) {
            if (isEnabled.getAsBoolean()) {
                return delegate.getRateLimiterOrDefault(key, resultIfNone);
            }
            return RateLimiter.NO_LIMIT;
        }
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RateLimiterFactoryWrapper that = (RateLimiterFactoryWrapper) o;
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
        public Matcher<HttpServletRequest> createMainMatcher(RateConfig rateConfig) {

            final String id = rateConfig.getId();

            // If no Matcher or a NO_OP Matcher exists, create new
            Matcher<HttpServletRequest> existing = registry.get(id).orElse(Matcher.matchNone());

            Matcher<HttpServletRequest> created = delegate.createMainMatcher(rateConfig);

            if (Matcher.matchNone().equals(existing)) {
                onMatcherCreated.accept(id, created);
                return created;
            }

            LOG.debug("Found existing matcher for {}, matcher: {}", id, existing);
            Matcher<HttpServletRequest> result = created == null ? existing : created.and(existing);
            onMatcherCreated.accept(id, result);
            return result;
        }
        @Override
        public List<Matcher<HttpServletRequest>> createSubMatchers(RateConfig rateConfig) {
            List<Matcher<HttpServletRequest>> result = delegate.createSubMatchers(rateConfig);
            result.forEach(matcher -> onMatcherCreated.accept(rateConfig.getId(), matcher));
            return result;
        }
    }
}
