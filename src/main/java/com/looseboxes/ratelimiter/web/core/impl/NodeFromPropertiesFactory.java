package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.NodeFactory;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class NodeFromPropertiesFactory implements NodeFactory<RateLimitProperties, RateConfigList> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromPropertiesFactory.class);

    @Override
    public Node<NodeData<RateConfigList>> createNode(
            String name,
            RateLimitProperties properties,
            BiConsumer<Object, Node<NodeData<RateConfigList>>> nodeConsumer) {

        final Node<NodeData<RateConfigList>> rootNode = addNodesToRoot(
                name, properties.getRateLimitConfigs(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }

    private Node<NodeData<RateConfigList>> addNodesToRoot(
            String rootNodeName,
            Map<String, RateConfigList> rateLimitConfigs,
            BiConsumer<Object, Node<NodeData<RateConfigList>>> nodeConsumer) {
        Map<String, RateConfigList> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        RateConfigList rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<RateConfigList> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<RateConfigList>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<RateConfigList>> parent,
            Map<String, RateConfigList> rateLimitConfigs,
            BiConsumer<Object, Node<NodeData<RateConfigList>>> nodeConsumer) {
        Set<Map.Entry<String, RateConfigList>> entrySet = rateLimitConfigs.entrySet();
        for (Map.Entry<String, RateConfigList> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            RateConfigList nodeConfig = entry.getValue();
            Node<NodeData<RateConfigList>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
