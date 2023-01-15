package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

abstract class NodeBuilder<S> {

    static NodeBuilder<List<Class<?>>> ofClasses(AnnotationProcessor<Class<?>> annotationProcessor) {
        return new ClassesToRatesNodeBuilder(annotationProcessor);
    }

    static NodeBuilder<RateLimitProperties> ofProperties() {
        return new PropertiesToRatesNodeBuilder();
    }

    private static final class ClassesToRatesNodeBuilder extends NodeBuilder<List<Class<?>>> {

        private static final Logger LOG = LoggerFactory.getLogger(ClassesToRatesNodeBuilder.class);

        private final AnnotationProcessor<Class<?>> annotationProcessor;

        private ClassesToRatesNodeBuilder(AnnotationProcessor<Class<?>> annotationProcessor) {
            this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
        }

        @Override
        public Node<RateConfig> buildNode(String name, List<Class<?>> sourceOfRateLimitInfo,
                                                AnnotationProcessor.NodeConsumer nodeConsumer) {

            Node<RateConfig> root = Node.of(name);

            root = annotationProcessor.processAll(root, nodeConsumer, sourceOfRateLimitInfo);

            if(LOG.isTraceEnabled()) {
                LOG.trace("Nodes:\n{}", root);
            }

            return root;
        }
    }

    private static final class PropertiesToRatesNodeBuilder extends NodeBuilder<RateLimitProperties> {

        private static final Logger LOG = LoggerFactory.getLogger(PropertiesToRatesNodeBuilder.class);

        PropertiesToRatesNodeBuilder() {}

        @Override
        public Node<RateConfig> buildNode(String name, RateLimitProperties sourceOfRateLimitInfo,
                                                AnnotationProcessor.NodeConsumer nodeConsumer) {

            final Node<RateConfig> rootNode = addNodesToRoot(
                    name, sourceOfRateLimitInfo.getRateLimitConfigs(), nodeConsumer);

            if(LOG.isTraceEnabled()) {
                LOG.trace("Nodes:\n{}", rootNode);
            }

            return rootNode;
        }

        private Node<RateConfig> addNodesToRoot(
                String rootNodeName,
                Map<String, Rates> limits,
                AnnotationProcessor.NodeConsumer nodeConsumer) {
            Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
            Rates rootNodeConfig = configsWithoutParent.remove(rootNodeName);
            RateConfig rateConfig = rootNodeConfig == null ? null : RateConfig
                    .of(rootNodeConfig, rootNodeConfig);
            Node<RateConfig> rootNode = Node.of(rootNodeName, rateConfig);
            nodeConsumer.accept(rootNodeConfig, rootNode);
            createNodes(rootNode, configsWithoutParent, nodeConsumer);
            return rootNode;
        }

        private void createNodes(
                Node<RateConfig> parent,
                Map<String, Rates> limits,
                AnnotationProcessor.NodeConsumer nodeConsumer) {
            Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
            for (Map.Entry<String, Rates> entry : entrySet) {
                String name = entry.getKey();
                Checks.requireParentNameDoesNotMatchChild(parent.getName(), name);
                Rates nodeConfig = entry.getValue();
                Node<RateConfig> node = Node.of(name, RateConfig.of(nodeConfig, nodeConfig), parent);
                nodeConsumer.accept(nodeConfig, node);
            }
        }
    }

    public Node<RateConfig> buildNode(String name, S source) {
        return buildNode(name, source, (src, node) -> {});
    }

    abstract Node<RateConfig> buildNode(String name, S source, AnnotationProcessor.NodeConsumer nodeConsumer);
}
