package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
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

            Node<RateConfig> rootNode = Node.of(name);

            annotationProcessor.processAll(rootNode, nodeConsumer, sourceOfRateLimitInfo);

            if(LOG.isTraceEnabled()) {
                LOG.trace("Element nodes: {}", NodeFormatter.indentedHeirarchy().format(rootNode));
            }

            return rootNode;
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
                LOG.trace("Element nodes: {}", NodeFormatter.indentedHeirarchy().format(rootNode));
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
                if(name.equals(parent.getName())) {
                    throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
                }
                Rates nodeConfig = entry.getValue();
                Node<RateConfig> node = Node.of(name, RateConfig.of(nodeConfig, nodeConfig), parent);
                nodeConsumer.accept(nodeConfig, node);
            }
        }
    }

    abstract Node<RateConfig> buildNode(String name, S source, AnnotationProcessor.NodeConsumer nodeConsumer);
}
