package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class PropertiesToRateLimitConfigNodeBuilder implements NodeBuilder<RateLimitProperties, Rates> {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesToRateLimitConfigNodeBuilder.class);
    
    public PropertiesToRateLimitConfigNodeBuilder() {}

    @Override
    public Node<NodeData<Rates>> buildNode(String name, RateLimitProperties sourceOfRateLimitInfo,
                                           BiConsumer<Object, Node<NodeData<Rates>>> nodeConsumer) {

        final Node<NodeData<Rates>> rootNode = addNodesToRoot(
                name, sourceOfRateLimitInfo.getRateLimitConfigs(), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }


    private Node<NodeData<Rates>> addNodesToRoot(
            String rootNodeName,
            Map<String, Rates> limits,
            BiConsumer<Object, Node<NodeData<Rates>>> nodeConsumer) {
        Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
        Rates rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<Rates> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<Rates>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<Rates>> parent,
            Map<String, Rates> limits,
            BiConsumer<Object, Node<NodeData<Rates>>> nodeConsumer) {
        Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
        for (Map.Entry<String, Rates> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            Rates nodeConfig = entry.getValue();
            Node<NodeData<Rates>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
