package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.RateLimiterProvider;
import io.github.poshjosh.ratelimiter.UsageListener;
import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.web.core.util.ResourceInfoProvider;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.*;

class ResourceLimiterConfigBuilder implements ResourceLimiterConfig.Builder {

    static final class ResourceLimiterConfigImpl implements ResourceLimiterConfig {

        private RateLimitProperties properties;
        private ResourceLimiterConfigurer configurer;
        private ClassesInPackageFinder classesInPackageFinder;
        private ResourceInfoProvider resourceInfoProvider;
        private ExpressionMatcher<HttpServletRequest, Object> expressionMatcher;
        private RateProcessor<Class<?>> classRateProcessor;
        private RateProcessor<RateLimitProperties> propertyRateProcessor;

        private MatcherProvider<HttpServletRequest> matcherProvider;

        private BandwidthsStore<?> store;

        private UsageListener usageListener;

        private RateLimiterProvider<HttpServletRequest, ?> rateLimiterProvider;

        @Override public RateLimitProperties getProperties() {
            return properties;
        }

        @Override public Optional<ResourceLimiterConfigurer> getConfigurer() {
            return Optional.ofNullable(configurer);
        }

        @Override public ClassesInPackageFinder getClassesInPackageFinder() {
            return classesInPackageFinder;
        }

        @Override public MatcherProvider<HttpServletRequest> getMatcherProvider() {
            return matcherProvider;
        }

        @Override public RateProcessor<Class<?>> getClassRateProcessor() {
            return classRateProcessor;
        }

        @Override public RateProcessor<RateLimitProperties> getPropertyRateProcessor() {
            return propertyRateProcessor;
        }

        @Override public BandwidthsStore<?> getStore() {
            return store;
        }

        @Override public UsageListener getUsageListener() {
            return usageListener;
        }

        @Override public RateLimiterProvider<HttpServletRequest, ?> getRateLimiterProvider() {
            return rateLimiterProvider;
        }
    }

    private final ResourceLimiterConfigImpl configuration;

    ResourceLimiterConfigBuilder() {
        this.configuration = new ResourceLimiterConfigImpl();
    }

    @Override
    public ResourceLimiterConfig build() {

        if (configuration.expressionMatcher == null) {
            expressionMatcher(WebExpressionMatcher.ofHttpServletRequest());
        }
        if (configuration.properties == null) {
            properties(new EmptyRateLimitProperties());
        }
        if (configuration.classesInPackageFinder == null) {
            classesInPackageFinder(ClassesInPackageFinder.ofDefaults());
        }
        if (configuration.classRateProcessor == null) {
            // We accept all class/method  nodes, even those without rate limit related annotations
            // This is because, any of the nodes may have its rate limit related info, specified
            // via properties. Such a node needs to be accepted at this point as property
            // sourced rate limited data will later be transferred to class/method nodes
            classRateProcessor(RateProcessor.ofClass(source -> true));
        }

        if (configuration.propertyRateProcessor == null) {
            propertyRateProcessor(new PropertyRateProcessor());
        }

        configuration.matcherProvider = new DefaultMatcherProvider(
                configuration.properties.getApplicationPath(),
                configuration.resourceInfoProvider,
                configuration.expressionMatcher);

        if (configuration.store == null) {
            store(BandwidthsStore.ofDefaults());
        }

        if (configuration.usageListener == null) {
            usageListener(UsageListener.NO_OP);
        }

        if (configuration.rateLimiterProvider == null) {
            rateLimiterProvider(RateLimiterProvider.of(configuration.store));
        }

        return configuration;
    }

    @Override public ResourceLimiterConfig.Builder properties(
            RateLimitProperties properties) {
        configuration.properties = properties;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder configurer(
            ResourceLimiterConfigurer configurer) {
        configuration.configurer = configurer;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder classesInPackageFinder(
            ClassesInPackageFinder classesInPackageFinder) {
        configuration.classesInPackageFinder = classesInPackageFinder;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder resourceInfoProvider(
            ResourceInfoProvider resourceInfoProvider) {
        configuration.resourceInfoProvider = resourceInfoProvider;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder expressionMatcher(
            ExpressionMatcher<HttpServletRequest, Object> expressionMatcher) {
        configuration.expressionMatcher = expressionMatcher;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder classRateProcessor(
            RateProcessor<Class<?>> rateProcessor) {
        configuration.classRateProcessor = rateProcessor;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder propertyRateProcessor(
            RateProcessor<RateLimitProperties> rateProcessor) {
        configuration.propertyRateProcessor = rateProcessor;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder store(BandwidthsStore<?> store) {
        configuration.store = store;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder addUsageListener(UsageListener listener) {
        if (configuration.usageListener == null) {
            usageListener(listener);
        } else {
            usageListener(configuration.usageListener.andThen(listener));
        }
        return this;
    }

    @Override public ResourceLimiterConfig.Builder usageListener(UsageListener listener) {
        configuration.usageListener = listener;
        return this;
    }

    @Override public ResourceLimiterConfig.Builder rateLimiterProvider(
            RateLimiterProvider<HttpServletRequest, ?> rateLimiterProvider) {
        configuration.rateLimiterProvider = rateLimiterProvider;
        return this;
    }

    private static final class EmptyRateLimitProperties implements RateLimitProperties {
        private EmptyRateLimitProperties() { }
        @Override public List<Class<?>> getResourceClasses() { return Collections.emptyList(); }
        @Override public List<String> getResourcePackages() {
            return Collections.emptyList();
        }
    }

    private static final class PropertyRateProcessor implements RateProcessor<RateLimitProperties> {
        private PropertyRateProcessor() { }
        @Override
        public Node<RateConfig> process(
                Node<RateConfig> root, NodeConsumer consumer, RateLimitProperties source) {
            return addNodesToRoot(root, source, consumer);
        }
        private Node<RateConfig> addNodesToRoot(
                Node<RateConfig> rootNode,
                RateLimitProperties source,
                NodeConsumer nodeConsumer) {
            Map<String, Rates> limits = source.getRateLimitConfigs();
            Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
            Rates rootNodeConfig = configsWithoutParent.remove(rootNode.getName());
            if (rootNodeConfig != null) {
                throw new IllegalStateException("The name: " + rootNode.getName() +
                        " is reserved, and may not be used to identify rates in " +
                        RateLimitProperties.class.getName());
            }
            nodeConsumer.accept(Rates.empty(), rootNode);
            createNodes(rootNode, nodeConsumer, source, configsWithoutParent);
            return rootNode;
        }
        private void createNodes(
                Node<RateConfig> parent,
                NodeConsumer nodeConsumer,
                RateLimitProperties source,
                Map<String, Rates> limits) {
            Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
            for (Map.Entry<String, Rates> entry : entrySet) {
                String name = entry.getKey();
                Checks.requireParentNameDoesNotMatchChild(parent.getName(), name);
                Rates rates = entry.getValue();
                RateSource rateSource = new PropertyRateSource(name, rates.hasLimits(), source);
                Node<RateConfig> node = Node.of(name, RateConfig.of(rateSource, rates), parent);
                nodeConsumer.accept(rates, node);
            }
        }
    }

    private static final class PropertyRateSource implements RateSource {
        private final String id;
        private final boolean rateLimited;
        private final RateLimitProperties source;
        public PropertyRateSource(String id, boolean rateLimited, RateLimitProperties source) {
            this.id = Objects.requireNonNull(id);
            this.rateLimited = rateLimited;
            this.source = Objects.requireNonNull(source);
        }
        @Override public Object getSource() { return source; }
        @Override public String getId() { return id; }
        @Override
        public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.empty();
        }
        @Override public boolean isRateLimited() { return rateLimited; }
        @Override public int hashCode() { return Objects.hashCode(getId()); }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PropertyRateSource)) {
                return false;
            }
            return getId().equals(((PropertyRateSource)o).getId());
        }
        @Override public String toString() {
            return this.getClass().getSimpleName() + '{' + getId() + '}';
        }
    }
}
