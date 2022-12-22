package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class PropertiesToRateLimitConfigNodeBuilder implements NodeBuilder<RateLimitProperties, RateLimitConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesToRateLimitConfigNodeBuilder.class);
    
    public PropertiesToRateLimitConfigNodeBuilder() {}

    @Override
    public Node<NodeData<RateLimitConfig>> buildNode(String name, RateLimitProperties sourceOfRateLimitInfo,
                                                     BiConsumer<Object, Node<NodeData<RateLimitConfig>>> nodeConsumer) {

        final Node<NodeData<RateLimitConfig>> rootNode = addNodesToRoot(
                name, sourceOfRateLimitInfo.getRateLimitConfigs(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }


    private Node<NodeData<RateLimitConfig>> addNodesToRoot(
            String rootNodeName,
            Map<String, RateLimitConfig> limits,
            BiConsumer<Object, Node<NodeData<RateLimitConfig>>> nodeConsumer) {
        Map<String, RateLimitConfig> configsWithoutParent = new LinkedHashMap<>(limits);
        RateLimitConfig rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<RateLimitConfig> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<RateLimitConfig>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<RateLimitConfig>> parent,
            Map<String, RateLimitConfig> limits,
            BiConsumer<Object, Node<NodeData<RateLimitConfig>>> nodeConsumer) {
        Set<Map.Entry<String, RateLimitConfig>> entrySet = limits.entrySet();
        for (Map.Entry<String, RateLimitConfig> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            RateLimitConfig nodeConfig = entry.getValue();
            Node<NodeData<RateLimitConfig>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
