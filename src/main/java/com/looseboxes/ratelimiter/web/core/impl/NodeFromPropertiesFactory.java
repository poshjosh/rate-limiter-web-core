package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.Limit;
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

public class NodeFromPropertiesFactory implements NodeFactory<RateLimitProperties, Limit> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromPropertiesFactory.class);

    @Override
    public Node<NodeData<Limit>> createNode(
            String name,
            RateLimitProperties properties,
            BiConsumer<Object, Node<NodeData<Limit>>> nodeConsumer) {

        final Node<NodeData<Limit>> rootNode = addNodesToRoot(
                name, properties.getLimits(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }

    private Node<NodeData<Limit>> addNodesToRoot(
            String rootNodeName,
            Map<String, Limit> limits,
            BiConsumer<Object, Node<NodeData<Limit>>> nodeConsumer) {
        Map<String, Limit> configsWithoutParent = new LinkedHashMap<>(limits);
        Limit rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<Limit> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<Limit>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<Limit>> parent,
            Map<String, Limit> limits,
            BiConsumer<Object, Node<NodeData<Limit>>> nodeConsumer) {
        Set<Map.Entry<String, Limit>> entrySet = limits.entrySet();
        for (Map.Entry<String, Limit> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            Limit nodeConfig = entry.getValue();
            Node<NodeData<Limit>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
