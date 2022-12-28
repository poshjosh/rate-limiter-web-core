package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class PropertiesToRatesNodeBuilder implements NodeBuilder<RateLimitProperties, Rates> {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesToRatesNodeBuilder.class);
    
    PropertiesToRatesNodeBuilder() {}

    @Override
    public Node<NodeValue<Rates>> buildNode(String name, RateLimitProperties sourceOfRateLimitInfo,
                                            AnnotationProcessor.NodeConsumer<Rates> nodeConsumer) {

        final Node<NodeValue<Rates>> rootNode = addNodesToRoot(
                name, sourceOfRateLimitInfo.getRateLimitConfigs(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }


    private Node<NodeValue<Rates>> addNodesToRoot(
            String rootNodeName,
            Map<String, Rates> limits,
            AnnotationProcessor.NodeConsumer<Rates> nodeConsumer) {
        Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
        Rates rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeValue<Rates> nodeValue = rootNodeConfig == null ? null : NodeValue.of(rootNodeConfig, rootNodeConfig);
        Node<NodeValue<Rates>> rootNode = Node.of(rootNodeName, nodeValue);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeValue<Rates>> parent,
            Map<String, Rates> limits,
            AnnotationProcessor.NodeConsumer<Rates> nodeConsumer) {
        Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
        for (Map.Entry<String, Rates> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            Rates nodeConfig = entry.getValue();
            Node<NodeValue<Rates>> node = Node.of(name, NodeValue.of(nodeConfig, nodeConfig), parent);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
