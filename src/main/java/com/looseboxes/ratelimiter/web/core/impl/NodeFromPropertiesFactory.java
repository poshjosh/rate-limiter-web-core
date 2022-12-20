package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.util.CompositeRate;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.web.core.NodeFactory;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class NodeFromPropertiesFactory implements NodeFactory<RateLimitProperties, CompositeRate> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromPropertiesFactory.class);

    @Override
    public Node<NodeData<CompositeRate>> createNode(
            String name,
            RateLimitProperties properties,
            BiConsumer<Object, Node<NodeData<CompositeRate>>> nodeConsumer) {

        final Node<NodeData<CompositeRate>> rootNode = addNodesToRoot(
                name, properties.getLimits(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }

    private Node<NodeData<CompositeRate>> addNodesToRoot(
            String rootNodeName,
            Map<String, CompositeRate> limits,
            BiConsumer<Object, Node<NodeData<CompositeRate>>> nodeConsumer) {
        Map<String, CompositeRate> configsWithoutParent = new LinkedHashMap<>(limits);
        CompositeRate rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<CompositeRate> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<CompositeRate>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<CompositeRate>> parent,
            Map<String, CompositeRate> limits,
            BiConsumer<Object, Node<NodeData<CompositeRate>>> nodeConsumer) {
        Set<Map.Entry<String, CompositeRate>> entrySet = limits.entrySet();
        for (Map.Entry<String, CompositeRate> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            CompositeRate nodeConfig = entry.getValue();
            Node<NodeData<CompositeRate>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
